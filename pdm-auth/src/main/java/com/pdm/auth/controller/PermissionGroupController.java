package com.pdm.auth.controller;

import com.pdm.auth.entity.PermissionGroup;
import com.pdm.auth.service.PermissionGroupService;
import com.pdm.common.core.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/permission-groups")
@RequiredArgsConstructor
public class PermissionGroupController {

    private final PermissionGroupService permissionGroupService;

    @GetMapping
    public Result<List<PermissionGroup>> listAll() {
        return Result.success(permissionGroupService.listAllGroups());
    }

    @PostMapping
    public Result<PermissionGroup> create(@RequestBody PermissionGroup group) {
        return Result.success(permissionGroupService.createGroup(group));
    }

    @PutMapping("/{id}")
    public Result<PermissionGroup> updatePermissions(
            @PathVariable Long id, @RequestBody PermissionGroup group) {
        return Result.success(
                permissionGroupService.updateGroupPermissions(id, group.getPermissions()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permissionGroupService.deleteGroup(id);
        return Result.success();
    }
}
