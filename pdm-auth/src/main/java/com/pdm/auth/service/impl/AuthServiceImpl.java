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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // Check account status
        checkAccountStatus(user);

        // Check lock
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int failCount = (user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount()) + 1;
            user.setFailedLoginCount(failCount);
            if (failCount >= BaseConstants.MAX_LOGIN_FAIL_COUNT) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(BaseConstants.LOGIN_LOCK_DURATION_MINUTES));
                user.setFailedLoginCount(0);
            }
            userMapper.updateById(user);
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // Login success — reset fail count & lock
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserUuid(), user.getUsername(),
                user.getUserRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserUuid());
        user.setToken(accessToken);
        userMapper.updateById(user);

        boolean mustChangePassword = user.getMustChangePassword() != null && user.getMustChangePassword();

        return LoginResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
                .expiresIn(BaseConstants.JWT_EXPIRATION_MS / 1000).userUuid(user.getUserUuid())
                .username(user.getUsername()).role(user.getUserRole()).mustChangePassword(mustChangePassword).build();
    }

    @Override
    public void logout(String token, String userUuid) {
        // Add token to blacklist in Redis
        long ttl = BaseConstants.JWT_EXPIRATION_MS / 1000;
        redisTemplate.opsForValue().set(BaseConstants.TOKEN_BLACKLIST_PREFIX + userUuid, token, ttl, TimeUnit.SECONDS);
    }

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

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUserUuid(), user.getUsername(),
                user.getUserRole());
        user.setToken(newAccessToken);
        userMapper.updateById(user);

        return LoginResponse.builder().accessToken(newAccessToken).expiresIn(BaseConstants.JWT_EXPIRATION_MS / 1000)
                .userUuid(user.getUserUuid()).username(user.getUsername()).role(user.getUserRole()).build();
    }

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

    @Override
    @Transactional
    public void registerUser(String userUuid, String username, String rawPassword, String phone, String residentUuid) {
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

    private void checkAccountStatus(User user) {
        switch (user.getAccountStatus()) {
            case "冻结" -> throw new BusinessException(ErrorCode.ACCOUNT_FROZEN);
            case "注销" -> throw new BusinessException(ErrorCode.ACCOUNT_CANCELLED);
            case "审批中" -> throw new BusinessException(ErrorCode.ACCOUNT_PENDING_APPROVAL);
            case "锁定" -> throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < BaseConstants.PASSWORD_MIN_LENGTH
                || password.length() > BaseConstants.PASSWORD_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK);
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars()
                .anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new BusinessException(ErrorCode.PASSWORD_WEAK);
        }
    }
}
