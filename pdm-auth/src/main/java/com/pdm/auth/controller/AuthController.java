package com.pdm.auth.controller;

import com.pdm.auth.service.AuthService;
import com.pdm.common.core.result.Result;
import com.pdm.common.dto.LoginRequest;
import com.pdm.common.dto.LoginResponse;
import com.pdm.common.security.UserContextHolder;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 认证控制器。
 *
 * <p>
 * 提供用户认证相关的 REST API 接口，包括登录、登出、令牌刷新和密码修改。 所有接口均挂载在 {@code /api/auth} 路径下。
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录。
     *
     * <p>
     * 校验用户名和密码，成功后返回 JWT 访问令牌和刷新令牌。 登录接口在安全配置中对所有用户放行。
     * </p>
     *
     * @param request
     *            登录请求体（含用户名和密码）
     * @param httpRequest
     *            HTTP 请求对象，用于获取客户端 IP
     * @return 包含访问令牌、刷新令牌及用户信息的登录响应
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return Result.success(response);
    }

    /**
     * 用户登出。
     *
     * <p>
     * 从请求头中提取 Bearer Token，调用服务层将其加入黑名单，使令牌在有效期内无法继续使用。
     * </p>
     *
     * @param authHeader
     *            Authorization 请求头（格式：{@code Bearer <token>}）
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token, UserContextHolder.getUserUuid());
        return Result.success();
    }

    /**
     * 刷新访问令牌。
     *
     * <p>
     * 使用有效的刷新令牌换取新的访问令牌。刷新接口在安全配置中对所有用户放行。
     * </p>
     *
     * @param body
     *            请求体，包含 {@code refreshToken} 字段
     * @return 包含新访问令牌的登录响应
     */
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        LoginResponse response = authService.refreshToken(body.get("refreshToken"));
        return Result.success(response);
    }

    /**
     * 修改密码。
     *
     * <p>
     * 需校验旧密码正确性，新密码须满足强度要求（大小写字母、数字、特殊字符）。
     * </p>
     *
     * @param body
     *            请求体，包含 {@code oldPassword} 和 {@code newPassword} 字段
     * @return 操作结果
     */
    @PutMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(UserContextHolder.getUserUuid(), body.get("oldPassword"), body.get("newPassword"));
        return Result.success();
    }
}
