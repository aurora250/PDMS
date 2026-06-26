package com.pdm.auth.service;

import com.pdm.auth.entity.PermissionGroup;

import java.util.List;

public interface PermissionGroupService {

    PermissionGroup createGroup(PermissionGroup group);

    PermissionGroup updateGroupPermissions(Long groupId, String permissions);

    void deleteGroup(Long groupId);

    List<PermissionGroup> listAllGroups();

    /**
     * 根据权限组ID获取用户的实际权限列表.
     *
     * @param groupId
     *            权限组ID，null 时返回空列表
     * @return 权限字符串列表，如 {@code ["resident:read","resident:write"]}；解析失败返回空列表
     */
    List<String> getPermissionsByGroupId(Long groupId);
}
