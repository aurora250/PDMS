package com.pdm.auth;

import com.pdm.auth.entity.User;
import com.pdm.auth.mapper.UserMapper;
import com.pdm.auth.service.impl.UserServiceImpl;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("用户管理服务 — 单元测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserUuid("00000000-0000-0000-0000-000000000001");
        testUser.setUsername("admin");
        testUser.setPhone("13800000000");
        testUser.setEmail("admin@pdm.gov.cn");
        testUser.setUserRole("系统管理员");
        testUser.setAccountStatus("有效");
        testUser.setPermissionGroupId(1L);
    }

    @Nested
    @DisplayName("查询用户列表")
    class ListUsers {

        @Test
        @DisplayName("无条件分页查询")
        void shouldListAllUsers() {
            Page<User> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testUser));
            mockPage.setTotal(1);

            when(userMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            Page<User> result = userService.listUsers(1, 20, null, null, null);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals("admin", result.getRecords().get(0).getUsername());
        }

        @Test
        @DisplayName("按用户名关键字筛选")
        void shouldFilterByKeyword() {
            Page<User> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testUser));
            mockPage.setTotal(1);

            when(userMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            Page<User> result = userService.listUsers(1, 20, "admin", null, null);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("按角色筛选")
        void shouldFilterByRole() {
            Page<User> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testUser));
            mockPage.setTotal(1);

            when(userMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            Page<User> result = userService.listUsers(1, 20, null, "系统管理员", null);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("按状态筛选")
        void shouldFilterByStatus() {
            Page<User> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testUser));
            mockPage.setTotal(1);

            when(userMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            Page<User> result = userService.listUsers(1, 20, null, null, "有效");

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("组合筛选条件")
        void shouldFilterByMultipleConditions() {
            Page<User> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testUser));
            mockPage.setTotal(1);

            when(userMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            Page<User> result = userService.listUsers(1, 20, "admin", "系统管理员", "有效");

            assertNotNull(result);
            assertEquals(1, result.getTotal());
        }

        @Test
        @DisplayName("无匹配结果返回空列表")
        void shouldReturnEmptyOnNoMatch() {
            Page<User> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of());
            mockPage.setTotal(0);

            when(userMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            Page<User> result = userService.listUsers(1, 20, "nonexistent", null, null);

            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("查询用户详情")
    class GetUserByUuid {

        @Test
        @DisplayName("通过UUID查询成功")
        void shouldFindByUuid() {
            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001"))
                    .thenReturn(testUser);

            User result = userService.getUserByUuid("00000000-0000-0000-0000-000000000001");

            assertNotNull(result);
            assertEquals("admin", result.getUsername());
            assertEquals("系统管理员", result.getUserRole());
        }

        @Test
        @DisplayName("UUID不存在应抛异常")
        void shouldThrowOnNonExistentUuid() {
            when(userMapper.selectByUserUuid("non-existent-uuid")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.getUserByUuid("non-existent-uuid"));
            assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("修改用户信息")
    class UpdateUser {

        @Test
        @DisplayName("正常修改用户信息")
        void shouldUpdateUserSuccessfully() {
            User updates = new User();
            updates.setPhone("13900001111");
            updates.setEmail("newemail@pdm.gov.cn");
            updates.setUserRole("数据审查员");
            updates.setPermissionGroupId(2L);

            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001"))
                    .thenReturn(testUser);

            User result = userService.updateUser("00000000-0000-0000-0000-000000000001", updates);

            assertNotNull(result);
            assertEquals("13900001111", result.getPhone());
            assertEquals("newemail@pdm.gov.cn", result.getEmail());
            assertEquals("数据审查员", result.getUserRole());
            assertEquals(2L, result.getPermissionGroupId());
            verify(userMapper).updateById(testUser);
        }

        @Test
        @DisplayName("部分字段更新，不影响其他字段")
        void shouldPartialUpdate() {
            User updates = new User();
            updates.setPhone("13900001111");
            // 其他字段为null

            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001"))
                    .thenReturn(testUser);

            User result = userService.updateUser("00000000-0000-0000-0000-000000000001", updates);

            assertEquals("13900001111", result.getPhone());
            assertEquals("admin", result.getUsername()); // 未变
        }

        @Test
        @DisplayName("用户不存在应抛异常")
        void shouldThrowWhenUserNotFound() {
            User updates = new User();
            updates.setPhone("13900001111");

            when(userMapper.selectByUserUuid("non-existent-uuid")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updateUser("non-existent-uuid", updates));
            assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
            verify(userMapper, never()).updateById(any(User.class));
        }
    }

    @Nested
    @DisplayName("更新用户状态")
    class UpdateUserStatus {

        @Test
        @DisplayName("设置为有效")
        void shouldSetToActive() {
            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001"))
                    .thenReturn(testUser);

            userService.updateUserStatus("00000000-0000-0000-0000-000000000001", "有效");

            assertEquals("有效", testUser.getAccountStatus());
            verify(userMapper).updateById(testUser);
        }

        @Test
        @DisplayName("设置为冻结")
        void shouldSetToFrozen() {
            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001"))
                    .thenReturn(testUser);

            userService.updateUserStatus("00000000-0000-0000-0000-000000000001", "冻结");

            assertEquals("冻结", testUser.getAccountStatus());
        }

        @Test
        @DisplayName("设置为锁定时自动设置解锁时间")
        void shouldSetToLockedWithUnlockTime() {
            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001"))
                    .thenReturn(testUser);

            userService.updateUserStatus("00000000-0000-0000-0000-000000000001", "锁定");

            assertEquals("锁定", testUser.getAccountStatus());
            assertNotNull(testUser.getLockedUntil());
            assertTrue(testUser.getLockedUntil().isAfter(LocalDateTime.now()));
        }

        @Test
        @DisplayName("设置为注销")
        void shouldSetToCancelled() {
            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001"))
                    .thenReturn(testUser);

            userService.updateUserStatus("00000000-0000-0000-0000-000000000001", "注销");

            assertEquals("注销", testUser.getAccountStatus());
        }

        @Test
        @DisplayName("设置为审批中")
        void shouldSetToPendingApproval() {
            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001"))
                    .thenReturn(testUser);

            userService.updateUserStatus("00000000-0000-0000-0000-000000000001", "审批中");

            assertEquals("审批中", testUser.getAccountStatus());
        }

        @Test
        @DisplayName("非法状态应抛异常")
        void shouldRejectInvalidStatus() {
            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001"))
                    .thenReturn(testUser);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updateUserStatus("00000000-0000-0000-0000-000000000001", "无效状态"));
            assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
            verify(userMapper, never()).updateById(any(User.class));
        }

        @Test
        @DisplayName("用户不存在时更新状态应抛异常")
        void shouldThrowWhenUserNotFound() {
            when(userMapper.selectByUserUuid("non-existent-uuid")).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> userService.updateUserStatus("non-existent-uuid", "有效"));
        }
    }

    @Nested
    @DisplayName("删除用户")
    class DeleteUser {

        @Test
        @DisplayName("删除非活跃状态用户成功")
        void shouldDeleteInactiveUser() {
            testUser.setAccountStatus("冻结");
            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001"))
                    .thenReturn(testUser);

            userService.deleteUser("00000000-0000-0000-0000-000000000001");

            verify(userMapper).deleteById(1L);
        }

        @Test
        @DisplayName("删除活跃用户应拒绝")
        void shouldRejectDeletingActiveUser() {
            testUser.setAccountStatus("有效");
            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001"))
                    .thenReturn(testUser);

            assertThrows(BusinessException.class,
                    () -> userService.deleteUser("00000000-0000-0000-0000-000000000001"));
            verify(userMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("用户不存在应抛异常")
        void shouldThrowWhenUserNotFound() {
            when(userMapper.selectByUserUuid("non-existent-uuid")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.deleteUser("non-existent-uuid"));
            assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
        }
    }
}
