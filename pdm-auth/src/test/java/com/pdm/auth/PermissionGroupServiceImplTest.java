package com.pdm.auth;

import com.pdm.auth.entity.PermissionGroup;
import com.pdm.auth.mapper.PermissionGroupMapper;
import com.pdm.auth.service.impl.PermissionGroupServiceImpl;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("权限组服务 — 单元测试")
class PermissionGroupServiceImplTest {

    @Mock
    private PermissionGroupMapper permissionGroupMapper;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private PermissionGroupServiceImpl permissionGroupService;

    private PermissionGroup testGroup;

    @BeforeEach
    void setUp() {
        testGroup = new PermissionGroup();
        testGroup.setGroupId(1L);
        testGroup.setGroupName("系统管理员组");
        testGroup.setPermissions("[\"*\"]");
    }

    @Nested
    @DisplayName("创建权限组")
    class CreateGroup {

        @Test
        @DisplayName("正常创建权限组")
        void shouldCreatePermissionGroup() {
            PermissionGroup newGroup = new PermissionGroup();
            newGroup.setGroupName("采集员组");
            newGroup.setPermissions("[\"resident:read\",\"resident:write\"]");

            PermissionGroup result = permissionGroupService.createGroup(newGroup);

            assertNotNull(result);
            assertEquals("采集员组", result.getGroupName());
            verify(permissionGroupMapper).insert(newGroup);
        }
    }

    @Nested
    @DisplayName("更新权限组权限")
    class UpdateGroupPermissions {

        @Test
        @DisplayName("正常更新权限")
        void shouldUpdatePermissions() {
            when(permissionGroupMapper.selectById(1L)).thenReturn(testGroup);

            PermissionGroup result = permissionGroupService.updateGroupPermissions(1L,
                    "[\"resident:read\",\"fp:read\"]");

            assertNotNull(result);
            assertEquals("[\"resident:read\",\"fp:read\"]", result.getPermissions());
            verify(permissionGroupMapper).updateById(testGroup);
        }

        @Test
        @DisplayName("权限组不存在应抛异常")
        void shouldThrowWhenGroupNotFound() {
            when(permissionGroupMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> permissionGroupService.updateGroupPermissions(999L, "[\"*\"]"));
            assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
            verify(permissionGroupMapper, never()).updateById(any(PermissionGroup.class));
        }
    }

    @Nested
    @DisplayName("删除权限组")
    class DeleteGroup {

        @Test
        @DisplayName("正常删除权限组")
        void shouldDeleteGroup() {
            when(permissionGroupMapper.selectById(1L)).thenReturn(testGroup);

            permissionGroupService.deleteGroup(1L);

            verify(permissionGroupMapper).deleteById(1L);
        }

        @Test
        @DisplayName("权限组不存在应抛异常")
        void shouldThrowWhenGroupNotFound() {
            when(permissionGroupMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> permissionGroupService.deleteGroup(999L));
            assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
            verify(permissionGroupMapper, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("查询所有权限组")
    class ListAllGroups {

        @Test
        @DisplayName("返回全部权限组列表")
        void shouldReturnAllGroups() {
            PermissionGroup group2 = new PermissionGroup();
            group2.setGroupId(2L);
            group2.setGroupName("采集员组");

            when(permissionGroupMapper.selectList(null)).thenReturn(List.of(testGroup, group2));

            List<PermissionGroup> result = permissionGroupService.listAllGroups();

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("无权限组时返回空列表")
        void shouldReturnEmptyList() {
            when(permissionGroupMapper.selectList(null)).thenReturn(List.of());

            List<PermissionGroup> result = permissionGroupService.listAllGroups();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("获取权限组的权限列表")
    class GetPermissionsByGroupId {

        @Test
        @DisplayName("正常解析权限JSON")
        void shouldParsePermissionsJson() throws JsonProcessingException {
            when(permissionGroupMapper.selectById(1L)).thenReturn(testGroup);
            when(objectMapper.readValue(eq("[\"*\"]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenReturn(List.of("*"));

            List<String> result = permissionGroupService.getPermissionsByGroupId(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("*", result.get(0));
        }

        @Test
        @DisplayName("groupId为null时返回空列表")
        void shouldReturnEmptyListForNullGroupId() {
            List<String> result = permissionGroupService.getPermissionsByGroupId(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verifyNoInteractions(permissionGroupMapper);
        }

        @Test
        @DisplayName("权限组不存在时返回空列表")
        void shouldReturnEmptyListWhenGroupNotFound() {
            when(permissionGroupMapper.selectById(999L)).thenReturn(null);

            List<String> result = permissionGroupService.getPermissionsByGroupId(999L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("权限JSON解析失败时返回空列表")
        void shouldReturnEmptyListOnParseFailure() throws JsonProcessingException {
            testGroup.setPermissions("invalid-json");
            when(permissionGroupMapper.selectById(1L)).thenReturn(testGroup);
            when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenThrow(new JsonProcessingException("Invalid JSON") {});

            List<String> result = permissionGroupService.getPermissionsByGroupId(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
