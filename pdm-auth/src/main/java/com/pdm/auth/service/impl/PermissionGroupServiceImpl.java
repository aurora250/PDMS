package com.pdm.auth.service.impl;

import com.pdm.auth.entity.PermissionGroup;
import com.pdm.auth.mapper.PermissionGroupMapper;
import com.pdm.auth.service.PermissionGroupService;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionGroupServiceImpl implements PermissionGroupService {

    private final PermissionGroupMapper permissionGroupMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PermissionGroup createGroup(PermissionGroup group) {
        permissionGroupMapper.insert(group);
        return group;
    }

    @Override
    @Transactional
    @CacheEvict(value = "permissions", key = "#groupId")
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

    @Override
    @Cacheable(value = "permissions", key = "#groupId", unless = "#result == null || #result.isEmpty()")
    public List<String> getPermissionsByGroupId(Long groupId) {
        if (groupId == null) {
            return Collections.emptyList();
        }
        PermissionGroup group = permissionGroupMapper.selectById(groupId);
        if (group == null || group.getPermissions() == null || group.getPermissions().isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(group.getPermissions(), new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("解析权限组[{}]的permissions JSON失败: {}", groupId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
