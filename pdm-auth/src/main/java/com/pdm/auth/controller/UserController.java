package com.pdm.auth.controller;

import com.pdm.auth.entity.User;
import com.pdm.auth.service.AuthService;
import com.pdm.auth.service.UserService;
import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping
    public Result<PageResult<User>> listUsers(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role, @RequestParam(required = false) String status) {
        Page<User> userPage = userService.listUsers(page, size, keyword, role, status);
        return Result.success(PageResult.of(userPage.getRecords(), userPage.getTotal(), page, size));
    }

    @PostMapping
    public Result<User> createUser(@RequestBody Map<String, Object> body) {
        String userUuid = body.containsKey("userUuid") ? (String) body.get("userUuid") : UUID.randomUUID().toString();
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String phone = (String) body.get("phone");
        String residentUuid = (String) body.get("residentUuid");

        authService.registerUser(userUuid, username, password, phone, residentUuid);

        // 注册后立即设置角色和权限组
        User updates = new User();
        if (body.containsKey("userRole")) {
            updates.setUserRole((String) body.get("userRole"));
        }
        if (body.containsKey("permissionGroupId")) {
            updates.setPermissionGroupId(((Number) body.get("permissionGroupId")).longValue());
        }
        if (body.containsKey("accountStatus")) {
            updates.setAccountStatus((String) body.get("accountStatus"));
        }
        return Result.success(userService.updateUser(userUuid, updates));
    }

    @GetMapping("/{uuid}")
    public Result<User> getUser(@PathVariable String uuid) {
        return Result.success(userService.getUserByUuid(uuid));
    }

    @PutMapping("/{uuid}")
    public Result<User> updateUser(@PathVariable String uuid, @RequestBody User updates) {
        return Result.success(userService.updateUser(uuid, updates));
    }

    @PutMapping("/{uuid}/status")
    public Result<Void> updateUserStatus(@PathVariable String uuid, @RequestBody Map<String, String> body) {
        userService.updateUserStatus(uuid, body.get("status"));
        return Result.success();
    }

    @DeleteMapping("/{uuid}")
    public Result<Void> deleteUser(@PathVariable String uuid) {
        userService.deleteUser(uuid);
        return Result.success();
    }
}
