package com.pdm.auth.controller;

import com.pdm.auth.entity.PermissionGroup;
import com.pdm.auth.service.PermissionGroupService;
import com.pdm.common.core.result.Result;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * 权限组管理控制器。
 *
 * <p>提供权限组的 CRUD REST API 接口，挂载在 {@code /api/auth/permission-groups} 路径下。 仅系统管理员角色可访问该控制器下的接口。
 */
@RestController
@RequestMapping("/api/auth/permission-groups")
@RequiredArgsConstructor
public class PermissionGroupController {

    private final PermissionGroupService permissionGroupService;

    /**
     * 查询全部权限组列表。
     *
     * @return 权限组列表
     */
    @GetMapping
    public Result<List<PermissionGroup>> listAll() {
        return Result.success(permissionGroupService.listAllGroups());
    }

    /**
     * 创建权限组。
     *
     * @param group 权限组实体（含名称、描述、权限 JSON）
     * @return 创建后的权限组（含自增主键）
     */
    @PostMapping
    public Result<PermissionGroup> create(@RequestBody PermissionGroup group) {
        return Result.success(permissionGroupService.createGroup(group));
    }

    /**
     * 更新权限组的权限列表。
     *
     * @param id 权限组 ID
     * @param group 包含新权限 JSON 的权限组对象
     * @return 更新后的权限组
     */
    @PutMapping("/{id}")
    public Result<PermissionGroup> updatePermissions(
            @PathVariable Long id, @RequestBody PermissionGroup group) {
        return Result.success(
                permissionGroupService.updateGroupPermissions(id, group.getPermissions()));
    }

    /**
     * 删除权限组。
     *
     * @param id 权限组 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permissionGroupService.deleteGroup(id);
        return Result.success();
    }
}
