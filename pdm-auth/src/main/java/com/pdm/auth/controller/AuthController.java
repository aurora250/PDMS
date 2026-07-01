package com.pdm.auth.controller;

import com.pdm.auth.service.AuthService;
import com.pdm.common.core.result.Result;
import com.pdm.common.dto.LoginRequest;
import com.pdm.common.dto.LoginResponse;
import com.pdm.common.security.UserContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return Result.success(response);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token, UserContextHolder.getUserUuid());
        return Result.success();
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        LoginResponse response = authService.refreshToken(body.get("refreshToken"));
        return Result.success(response);
    }

    @PutMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(UserContextHolder.getUserUuid(), body.get("oldPassword"), body.get("newPassword"));
        return Result.success();
    }

    /**
     * 公众自助注册（无需登录）。群众/采集员/街道办通过实名认证注册账号， 非群众角色需附加材料，注册后状态为"审批中"，由管理员审核后生效。
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody Map<String, Object> body) {
        String userUuid = body.containsKey("userUuid") ? (String) body.get("userUuid") : UUID.randomUUID().toString();
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String phone = (String) body.get("phone");
        String residentUuid = (String) body.get("residentUuid");
        String userRole = body.containsKey("userRole") ? (String) body.get("userRole") : "普通用户";

        // 非群众角色需附加审核材料
        String registerMaterials = "[]";
        if (body.containsKey("registerMaterials")) {
            Object materials = body.get("registerMaterials");
            try {
                registerMaterials = (materials instanceof String)
                        ? (String) materials
                        : new ObjectMapper().writeValueAsString(materials);
            } catch (Exception ignored) {
                // ignore serialization failures
            }
        }

        // 一次写入：注册用户（含角色、材料、审批状态），无需二次 updateUser
        authService.registerUser(userUuid, username, password, phone, residentUuid, userRole, registerMaterials);

        return Result.success(userUuid);
    }
}
