package com.pdm.auth.service.impl;

import com.pdm.auth.entity.PermissionGroup;
import com.pdm.auth.mapper.PermissionGroupMapper;
import com.pdm.auth.service.PermissionGroupService;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionGroupServiceImpl implements PermissionGroupService {

    private final PermissionGroupMapper permissionGroupMapper;

    @Override
    @Transactional
    public PermissionGroup createGroup(PermissionGroup group) {
        permissionGroupMapper.insert(group);
        return group;
    }

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

    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        PermissionGroup group = permissionGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "权限组不存在");
        }
        permissionGroupMapper.deleteById(groupId);
    }

    @Override
    public List<PermissionGroup> listAllGroups() {
        return permissionGroupMapper.selectList(null);
    }
}
