package com.pdm.auth.controller;

import com.pdm.auth.entity.User;
import com.pdm.auth.service.UserService;
import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * 用户管理控制器。
 *
 * <p>
 * 提供用户的查询、更新、状态管理和删除 REST API 接口，挂载在 {@code /api/auth/users} 路径下。
 * 需要系统管理员或用户管理员角色方可访问。
 * </p>
 */
@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表。
     *
     * <p>
     * 支持按关键词（用户名/UUID）、角色和状态进行多条件筛选。
     * </p>
     *
     * @param page
     *            页码，默认 1
     * @param size
     *            每页条数，默认 20
     * @param keyword
     *            搜索关键词（可选，模糊匹配用户名或 UUID）
     * @param role
     *            角色筛选（可选）
     * @param status
     *            状态筛选（可选）
     * @return 用户分页结果
     */
    @GetMapping
    public Result<PageResult<User>> listUsers(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role, @RequestParam(required = false) String status) {
        Page<User> userPage = userService.listUsers(page, size, keyword, role, status);
        return Result.success(PageResult.of(userPage.getRecords(), userPage.getTotal(), page, size));
    }

    /**
     * 根据用户 UUID 查询用户详情。
     *
     * @param uuid
     *            用户唯一标识
     * @return 用户信息
     */
    @GetMapping("/{uuid}")
    public Result<User> getUser(@PathVariable String uuid) {
        return Result.success(userService.getUserByUuid(uuid));
    }

    /**
     * 更新用户信息。
     *
     * <p>
     * 仅更新传入对象中非空的字段（手机、邮箱、角色、权限组）。
     * </p>
     *
     * @param uuid
     *            用户唯一标识
     * @param updates
     *            包含待更新字段的用户对象
     * @return 更新后的用户信息
     */
    @PutMapping("/{uuid}")
    public Result<User> updateUser(@PathVariable String uuid, @RequestBody User updates) {
        return Result.success(userService.updateUser(uuid, updates));
    }

    /**
     * 更新用户账号状态。
     *
     * <p>
     * 支持设置为"审批中""有效""冻结""注销""锁定"五种状态， 设为"锁定"时自动设置 24 小时后解锁。
     * </p>
     *
     * @param uuid
     *            用户唯一标识
     * @param body
     *            请求体，包含 {@code status} 字段
     * @return 操作结果
     */
    @PutMapping("/{uuid}/status")
    public Result<Void> updateUserStatus(@PathVariable String uuid, @RequestBody Map<String, String> body) {
        userService.updateUserStatus(uuid, body.get("status"));
        return Result.success();
    }

    /**
     * 删除用户。
     *
     * <p>
     * 仅允许删除非"有效"状态的用户，活跃用户需先停用后再删除。
     * </p>
     *
     * @param uuid
     *            用户唯一标识
     * @return 操作结果
     */
    @DeleteMapping("/{uuid}")
    public Result<Void> deleteUser(@PathVariable String uuid) {
        userService.deleteUser(uuid);
        return Result.success();
    }
}
