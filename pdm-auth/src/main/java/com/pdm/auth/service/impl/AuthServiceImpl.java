package com.pdm.auth.service.impl;

import com.pdm.auth.entity.User;
import com.pdm.auth.mapper.UserMapper;
import com.pdm.auth.service.AuthService;
import com.pdm.common.core.constant.BaseConstants;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.common.dto.LoginRequest;
import com.pdm.common.dto.LoginResponse;
import com.pdm.common.security.JwtTokenProvider;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证服务实现类，提供用户登录、登出、令牌刷新、密码修改和用户注册等核心认证功能。
 *
 * <ul>
 *   <li><b>登录</b> —— 校验账号状态与密码，失败达上限后锁定账号；成功后生成 JWT 访问令牌和刷新令牌。
 *   <li><b>登出</b> —— 将当前令牌写入 Redis 黑名单，TTL 与 JWT 过期时间一致，使其在有效期内无法继续使用。
 *   <li><b>令牌刷新</b> —— 校验刷新令牌有效性后生成新的访问令牌。
 *   <li><b>密码修改</b> —— 校验旧密码，验证新密码强度（大小写字母+数字+特殊字符），加密后保存。
 *   <li><b>用户注册</b> —— 检查用户名唯一性，校验密码强度，默认角色为"普通用户"、状态为"审批中"。
 * </ul>
 *
 * @author freedom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 用户登录。
     *
     * <p>根据用户名查询用户，依次校验账号状态、锁定状态和密码。 密码错误时累加失败次数，达到 {@link BaseConstants#MAX_LOGIN_FAIL_COUNT}
     * 后锁定账号 {@link BaseConstants#LOGIN_LOCK_DURATION_MINUTES} 分钟。 登录成功后重置失败计数与锁定状态，记录本次登录时间和 IP，生成
     * JWT 令牌并返回。
     *
     * @param request 登录请求（用户名 + 密码）
     * @param ipAddress 客户端 IP 地址
     * @return 登录响应，含访问令牌、刷新令牌、过期时间及用户基本信息
     * @throws BusinessException 用户名或密码错误、账号被冻结/注销/锁定/审批中时抛出
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        checkAccountStatus(user);

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int failCount =
                    (user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount()) + 1;
            user.setFailedLoginCount(failCount);
            if (failCount >= BaseConstants.MAX_LOGIN_FAIL_COUNT) {
                user.setLockedUntil(
                        LocalDateTime.now().plusMinutes(BaseConstants.LOGIN_LOCK_DURATION_MINUTES));
                user.setFailedLoginCount(0);
            }
            userMapper.updateById(user);
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);

        String accessToken =
                jwtTokenProvider.generateAccessToken(
                        user.getUserUuid(), user.getUsername(), user.getUserRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserUuid());
        user.setToken(accessToken);
        userMapper.updateById(user);

        boolean mustChangePassword =
                user.getMustChangePassword() != null && user.getMustChangePassword();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(BaseConstants.JWT_EXPIRATION_MS / 1000)
                .userUuid(user.getUserUuid())
                .username(user.getUsername())
                .role(user.getUserRole())
                .mustChangePassword(mustChangePassword)
                .build();
    }

    /**
     * 用户登出。
     *
     * <p>将当前访问令牌以 {@code userUuid} 为键写入 Redis 黑名单， 过期时间与 JWT 有效期一致，使令牌在有效期内无法继续使用。
     *
     * @param token 需要失效的访问令牌
     * @param userUuid 用户唯一标识
     */
    @Override
    public void logout(String token, String userUuid) {
        long ttl = BaseConstants.JWT_EXPIRATION_MS / 1000;
        redisTemplate
                .opsForValue()
                .set(BaseConstants.TOKEN_BLACKLIST_PREFIX + userUuid, token, ttl, TimeUnit.SECONDS);
    }

    /**
     * 刷新访问令牌。
     *
     * <p>校验刷新令牌有效性，从中提取用户 UUID 并查询用户，随后生成新的访问令牌返回。 注意：此操作不会返回新的刷新令牌。
     *
     * @param refreshToken 刷新令牌
     * @return 包含新访问令牌的登录响应
     * @throws BusinessException 令牌无效或用户不存在时抛出
     */
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        String userUuid = jwtTokenProvider.getUserUuid(refreshToken);
        User user = userMapper.selectByUserUuid(userUuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }

        String newAccessToken =
                jwtTokenProvider.generateAccessToken(
                        user.getUserUuid(), user.getUsername(), user.getUserRole());
        user.setToken(newAccessToken);
        userMapper.updateById(user);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .expiresIn(BaseConstants.JWT_EXPIRATION_MS / 1000)
                .userUuid(user.getUserUuid())
                .username(user.getUsername())
                .role(user.getUserRole())
                .build();
    }

    /**
     * 修改密码。
     *
     * <p>校验旧密码正确后，对新密码进行强度验证（长度、大小写字母、数字、特殊字符）， 加密后保存并清除"强制修改密码"标记。
     *
     * @param userUuid 用户唯一标识
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     * @throws BusinessException 用户不存在、旧密码错误或新密码强度不足时抛出
     * @see #validatePasswordStrength(String)
     */
    @Override
    @Transactional
    public void changePassword(String userUuid, String oldPassword, String newPassword) {
        User user = userMapper.selectByUserUuid(userUuid);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        validatePasswordStrength(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userMapper.updateById(user);
    }

    /**
     * 注册新用户。
     *
     * <p>检查用户名唯一性并验证密码强度后创建用户。 新用户默认角色为"普通用户"、账号状态为"审批中"，密码加密存储， 且首次登录需修改密码。
     *
     * @param userUuid 用户唯一标识（为 {@code null} 时使用默认值）
     * @param username 用户名
     * @param rawPassword 明文密码
     * @param phone 手机号
     * @param residentUuid 关联居民 UUID
     * @throws BusinessException 用户名已存在或密码强度不足时抛出
     */
    @Override
    @Transactional
    public void registerUser(
            String userUuid,
            String username,
            String rawPassword,
            String phone,
            String residentUuid) {
        if (userMapper.countByUsername(username) > 0) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATE, "用户名已存在");
        }
        validatePasswordStrength(rawPassword);

        User user = new User();
        user.setUserUuid(userUuid != null ? userUuid : "test-uuid");
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setResidentUuid(residentUuid);
        user.setUserRole("普通用户");
        user.setAccountStatus("审批中");
        user.setMustChangePassword(true);
        user.setFailedLoginCount(0);
        userMapper.insert(user);
    }

    /**
     * 检查账号状态，对异常状态抛出对应异常。
     *
     * <ul>
     *   <li>{@code "冻结"} —— 账号已被冻结
     *   <li>{@code "注销"} —— 账号已注销
     *   <li>{@code "审批中"} —— 账号待审批，暂不可登录
     *   <li>{@code "锁定"} —— 账号已被锁定
     * </ul>
     *
     * @param user 用户对象
     * @throws BusinessException 账号状态为上述四种之一时抛出
     */
    private void checkAccountStatus(User user) {
        switch (user.getAccountStatus()) {
            case "冻结" -> throw new BusinessException(ErrorCode.ACCOUNT_FROZEN);
            case "注销" -> throw new BusinessException(ErrorCode.ACCOUNT_CANCELLED);
            case "审批中" -> throw new BusinessException(ErrorCode.ACCOUNT_PENDING_APPROVAL);
            case "锁定" -> throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    /**
     * 验证密码强度。
     *
     * <p>密码必须同时满足以下条件：
     *
     * <ul>
     *   <li>长度在 {@link BaseConstants#PASSWORD_MIN_LENGTH} ~ {@link
     *       BaseConstants#PASSWORD_MAX_LENGTH} 之间
     *   <li>至少包含一个大写字母
     *   <li>至少包含一个小写字母
     *   <li>至少包含一个数字
     *   <li>至少包含一个特殊字符（非字母、非数字、非空白）
     * </ul>
     *
     * @param password 待验证的明文密码
     * @throws BusinessException 密码为 {@code null}、长度不符或缺少所需字符类型时抛出
     */
    private void validatePasswordStrength(String password) {
        if (password == null
                || password.length() < BaseConstants.PASSWORD_MIN_LENGTH
                || password.length() > BaseConstants.PASSWORD_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK);
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial =
                password.chars()
                        .anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK);
        }
    }
}
