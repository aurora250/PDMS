package com.pdm.auth.service.impl;

import com.pdm.auth.entity.PermissionGroup;
import com.pdm.auth.mapper.PermissionGroupMapper;
import com.pdm.auth.service.PermissionGroupService;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * 权限组服务实现类，提供权限组的增删改查功能。
 *
 * <ul>
 *   <li><b>创建权限组</b> —— 新增一个权限组记录。
 *   <li><b>更新权限</b> —— 修改指定权限组的权限 JSON 字符串。
 *   <li><b>删除权限组</b> —— 根据 ID 删除权限组，不存在时抛出异常。
 *   <li><b>查询全部</b> —— 获取所有权限组列表。
 * </ul>
 *
 * @author freedom
 */
@Service
@RequiredArgsConstructor
public class PermissionGroupServiceImpl implements PermissionGroupService {

    private final PermissionGroupMapper permissionGroupMapper;

    /**
     * 创建权限组。
     *
     * <p>将传入的 {@link PermissionGroup} 对象插入数据库并返回。 插入后实体中的自增主键 {@code groupId} 会被回填。
     *
     * @param group 待创建的权限组实体（含名称、描述、权限 JSON）
     * @return 创建后的权限组实体（含自增主键）
     */
    @Override
    @Transactional
    public PermissionGroup createGroup(PermissionGroup group) {
        permissionGroupMapper.insert(group);
        return group;
    }

    /**
     * 更新权限组的权限字符串。
     *
     * <p>根据权限组 ID 查询记录，存在则更新其 {@code permissions} 字段（JSON 格式的权限数组），不存在则抛出异常。
     *
     * @param groupId 权限组 ID
     * @param permissions 新的权限 JSON 字符串
     * @return 更新后的权限组实体
     * @throws BusinessException 权限组不存在时抛出
     */
    @Override
    @Transactional
    public PermissionGroup updateGroupPermissions(Long groupId, String permissions) {
        PermissionGroup group = permissionGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "权限组不存在");
        }
        group.setPermissions(permissions);
        permissionGroupMapper.updateById(group);
        return group;
    }

    /**
     * 删除权限组。
     *
     * <p>根据 ID 查询权限组，存在则删除，不存在则抛出异常。
     *
     * @param groupId 权限组 ID
     * @throws BusinessException 权限组不存在时抛出
     */
    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        PermissionGroup group = permissionGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "权限组不存在");
        }
        permissionGroupMapper.deleteById(groupId);
    }

    /**
     * 查询全部权限组。
     *
     * @return 所有权限组列表，无数据时返回空列表
     */
    @Override
    public List<PermissionGroup> listAllGroups() {
        return permissionGroupMapper.selectList(null);
    }
}
