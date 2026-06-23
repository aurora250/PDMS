package com.pdm.auth.controller;

import com.pdm.auth.entity.User;
import com.pdm.auth.service.UserService;
import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<PageResult<User>> listUsers(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role, @RequestParam(required = false) String status) {
        Page<User> userPage = userService.listUsers(page, size, keyword, role, status);
        return Result.success(PageResult.of(userPage.getRecords(), userPage.getTotal(), page, size));
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
