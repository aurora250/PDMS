package com.pdm.auth;

import com.pdm.auth.entity.User;
import com.pdm.auth.mapper.UserMapper;
import com.pdm.auth.service.impl.AuthServiceImpl;
import com.pdm.common.core.constant.BaseConstants;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.common.dto.LoginRequest;
import com.pdm.common.dto.LoginResponse;
import com.pdm.common.security.JwtTokenProvider;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("认证服务 — 单元测试")
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserUuid("00000000-0000-0000-0000-000000000001");
        testUser.setUsername("admin");
        testUser.setPassword("$2a$10$encryptedPassword");
        testUser.setUserRole("系统管理员");
        testUser.setAccountStatus("有效");
        testUser.setPhone("13800000000");
        testUser.setFailedLoginCount(0);
        testUser.setMustChangePassword(false);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("登录测试")
    class LoginTests {

        @Test
        @DisplayName("正常登录成功")
        void shouldLoginSuccessfully() {
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("Admin@123!");

            when(userMapper.selectByUsername("admin")).thenReturn(testUser);
            when(passwordEncoder.matches("Admin@123!", testUser.getPassword())).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken("00000000-0000-0000-0000-000000000001", "admin", "系统管理员")).thenReturn("access-token-xxx");
            when(jwtTokenProvider.generateRefreshToken("00000000-0000-0000-0000-000000000001")).thenReturn("refresh-token-xxx");

            LoginResponse response = authService.login(request, "127.0.0.1");

            assertNotNull(response);
            assertEquals("access-token-xxx", response.getAccessToken());
            assertEquals("refresh-token-xxx", response.getRefreshToken());
            assertEquals("00000000-0000-0000-0000-000000000001", response.getUserUuid());
            assertEquals("admin", response.getUsername());
            assertEquals("系统管理员", response.getRole());
            assertFalse(response.isMustChangePassword());
            assertEquals("127.0.0.1", testUser.getLastLoginIp());
            assertNotNull(testUser.getLastLoginTime());

            verify(userMapper).updateById(testUser);
        }

        @Test
        @DisplayName("密码错误应抛出异常")
        void shouldFailOnWrongPassword() {
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("WrongPassword");

            when(userMapper.selectByUsername("admin")).thenReturn(testUser);
            when(passwordEncoder.matches("WrongPassword", testUser.getPassword())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request, "127.0.0.1"));
            assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("用户不存在应抛出异常")
        void shouldFailOnNonExistentUser() {
            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent");
            request.setPassword("any");

            when(userMapper.selectByUsername("nonexistent")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request, "127.0.0.1"));
            assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("冻结账号无法登录")
        void shouldRejectFrozenAccount() {
            testUser.setAccountStatus("冻结");
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("Admin@123!");

            when(userMapper.selectByUsername("admin")).thenReturn(testUser);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request, "127.0.0.1"));
            assertEquals(ErrorCode.ACCOUNT_FROZEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("锁定账号在锁定期内无法登录")
        void shouldRejectLockedAccount() {
            testUser.setLockedUntil(LocalDateTime.now().plusMinutes(10));
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("Admin@123!");

            when(userMapper.selectByUsername("admin")).thenReturn(testUser);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request, "127.0.0.1"));
            assertEquals(ErrorCode.ACCOUNT_LOCKED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("连续5次失败后账号被锁定30分钟")
        void shouldLockAfter5FailedAttempts() {
            testUser.setFailedLoginCount(4);
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("WrongPassword");

            when(userMapper.selectByUsername("admin")).thenReturn(testUser);
            when(passwordEncoder.matches("WrongPassword", testUser.getPassword())).thenReturn(false);

            assertThrows(BusinessException.class, () -> authService.login(request, "127.0.0.1"));

            // Should be locked now
            assertNotNull(testUser.getLockedUntil());
            assertTrue(testUser.getLockedUntil().isAfter(LocalDateTime.now()));
            verify(userMapper, atLeastOnce()).updateById(testUser);
        }

        @Test
        @DisplayName("首次登录需要强制改密")
        void shouldFlagMustChangePassword() {
            testUser.setMustChangePassword(true);
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("Admin@123!");

            when(userMapper.selectByUsername("admin")).thenReturn(testUser);
            when(passwordEncoder.matches("Admin@123!", testUser.getPassword())).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(anyString(), anyString(), anyString()))
                    .thenReturn("access-token-xxx");
            when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh-token-xxx");

            LoginResponse response = authService.login(request, "127.0.0.1");
            assertTrue(response.isMustChangePassword());
        }
    }

    @Nested
    @DisplayName("修改密码")
    class ChangePasswordTests {

        @Test
        @DisplayName("合法密码修改成功")
        void shouldChangePassword() {
            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001")).thenReturn(testUser);
            when(passwordEncoder.matches("OldPass1!", testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("NewPass2@")).thenReturn("$2a$10$newlyEncoded");

            authService.changePassword("00000000-0000-0000-0000-000000000001", "OldPass1!", "NewPass2@");

            assertEquals("$2a$10$newlyEncoded", testUser.getPassword());
            assertFalse(testUser.getMustChangePassword());
            verify(userMapper).updateById(testUser);
        }

        @Test
        @DisplayName("弱密码应拒绝")
        void shouldRejectWeakPassword() {
            when(userMapper.selectByUserUuid("00000000-0000-0000-0000-000000000001")).thenReturn(testUser);
            when(passwordEncoder.matches("OldPass1!", testUser.getPassword())).thenReturn(true);

            assertThrows(BusinessException.class, () -> authService.changePassword("00000000-0000-0000-0000-000000000001", "OldPass1!", "short"));
            assertThrows(BusinessException.class,
                    () -> authService.changePassword("00000000-0000-0000-0000-000000000001", "OldPass1!", "nouppercase1!"));
            assertThrows(BusinessException.class,
                    () -> authService.changePassword("00000000-0000-0000-0000-000000000001", "OldPass1!", "NOLOWERCASE1!"));
        }
    }

    @Nested
    @DisplayName("登出")
    class LogoutTests {

        @Test
        @DisplayName("登出将Token加入黑名单")
        void shouldBlacklistTokenOnLogout() {
            authService.logout("token-xxx", "00000000-0000-0000-0000-000000000001");

            verify(valueOperations).set(eq(BaseConstants.TOKEN_BLACKLIST_PREFIX + "00000000-0000-0000-0000-000000000001"), eq("token-xxx"),
                    anyLong(), eq(TimeUnit.SECONDS));
        }
    }
}
