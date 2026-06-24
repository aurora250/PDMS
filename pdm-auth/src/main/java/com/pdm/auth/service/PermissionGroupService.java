package com.pdm.auth.service;

import com.pdm.auth.entity.PermissionGroup;

import java.util.List;

public interface PermissionGroupService {

    PermissionGroup createGroup(PermissionGroup group);

    PermissionGroup updateGroupPermissions(Long groupId, String permissions);

    void deleteGroup(Long groupId);

    List<PermissionGroup> listAllGroups();
}
