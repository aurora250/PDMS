package com.pdm.auth.service;

import com.pdm.auth.entity.PermissionGroup;

import java.util.List;

/**
 * 权限组服务接口。
 *
 * <p>
 * 定义权限组的增删改查操作，支持创建、更新权限、删除和全量查询。
 * </p>
 */
public interface PermissionGroupService {

    /**
     * 创建权限组。
     *
     * @param group
     *            待创建的权限组实体
     * @return 创建后的权限组（含回填的主键 ID）
     */
    PermissionGroup createGroup(PermissionGroup group);

    /**
     * 更新权限组的权限列表。
     *
     * @param groupId
     *            权限组 ID
     * @param permissions
     *            新的权限 JSON 字符串
     * @return 更新后的权限组实体
     */
    PermissionGroup updateGroupPermissions(Long groupId, String permissions);

    /**
     * 删除权限组。
     *
     * @param groupId
     *            权限组 ID
     */
    void deleteGroup(Long groupId);

    /**
     * 查询全部权限组。
     *
     * @return 所有权限组列表
     */
    List<PermissionGroup> listAllGroups();
}
