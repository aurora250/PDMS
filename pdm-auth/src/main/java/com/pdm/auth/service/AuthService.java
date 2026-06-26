package com.pdm.auth.service;

import com.pdm.common.dto.LoginRequest;
import com.pdm.common.dto.LoginResponse;

/**
 * 认证服务接口。
 *
 * <p>定义用户认证相关的核心业务操作，包括登录、登出、令牌刷新、密码修改和用户注册。
 */
public interface AuthService {

    /**
     * 用户登录。
     *
     * @param request 登录请求（用户名 + 密码）
     * @param ipAddress 客户端 IP 地址
     * @return 登录响应，含 JWT 令牌及用户信息
     */
    LoginResponse login(LoginRequest request, String ipAddress);

    /**
     * 用户登出，将当前令牌加入黑名单使其失效。
     *
     * @param token 需要失效的访问令牌
     * @param userUuid 用户唯一标识
     */
    void logout(String token, String userUuid);

    /**
     * 刷新访问令牌。
     *
     * @param refreshToken 刷新令牌
     * @return 包含新访问令牌的登录响应
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 修改用户密码。
     *
     * @param userUuid 用户唯一标识
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     */
    void changePassword(String userUuid, String oldPassword, String newPassword);

    /**
     * 注册新用户。
     *
     * @param userUuid 用户唯一标识（可为 null，由系统自动生成）
     * @param username 用户名
     * @param rawPassword 明文密码
     * @param phone 手机号
     * @param residentUuid 关联的居民 UUID
     */
    void registerUser(
            String userUuid,
            String username,
            String rawPassword,
            String phone,
            String residentUuid);
}
