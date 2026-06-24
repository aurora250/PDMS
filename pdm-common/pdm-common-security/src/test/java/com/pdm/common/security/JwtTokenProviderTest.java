package com.pdm.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;

@DisplayName("JWT Token 提供器")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String base64Secret;
    private byte[] secretBytes;

    @BeforeEach
    void setUp() {
        secretBytes = new byte[32]; // 256 bits for HS256
        new SecureRandom().nextBytes(secretBytes);
        base64Secret = Base64.getEncoder().encodeToString(secretBytes);
        jwtTokenProvider = new JwtTokenProvider(base64Secret);
    }

    private SecretKey generateKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Keys.hmacShaKeyFor(key);
    }

    @Test
    @DisplayName("生成并验证Access Token")
    void shouldGenerateAndValidateAccessToken() {
        String token = jwtTokenProvider.generateAccessToken("user-uuid-001", "admin", "系统管理员");
        assertNotNull(token);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("user-uuid-001", jwtTokenProvider.getUserUuid(token));
        assertEquals("admin", jwtTokenProvider.getUsername(token));
        assertEquals("系统管理员", jwtTokenProvider.getRole(token));
    }

    @Test
    @DisplayName("无效Token验证失败")
    void shouldRejectInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("篡改Token验证失败")
    void shouldRejectTamperedToken() {
        String token = jwtTokenProvider.generateAccessToken("user-uuid-001", "admin", "系统管理员");
        String tampered = token.substring(0, token.length() - 1) + "X";
        assertFalse(jwtTokenProvider.validateToken(tampered));
    }

    @Test
    @DisplayName("使用不同密钥生成的Token验证失败")
    void shouldRejectTokenFromDifferentKey() {
        SecretKey otherKey = generateKey();
        String otherBase64 = Base64.getEncoder().encodeToString(otherKey.getEncoded());
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherBase64);
        String token = otherProvider.generateAccessToken("user-001", "admin", "系统管理员");

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("生成Refresh Token")
    void shouldGenerateRefreshToken() {
        String refreshToken = jwtTokenProvider.generateRefreshToken("user-uuid-001");
        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
        assertEquals("user-uuid-001", jwtTokenProvider.getUserUuid(refreshToken));
    }

    @Test
    @DisplayName("Token即将过期检测")
    void shouldDetectExpirySoon() {
        JwtTokenProvider shortProvider = new JwtTokenProvider(base64Secret, 100L);
        String token = shortProvider.generateAccessToken("user-001", "admin", "系统管理员");
        assertTrue(shortProvider.isTokenExpiringSoon(token));

        assertFalse(jwtTokenProvider
                .isTokenExpiringSoon(jwtTokenProvider.generateAccessToken("user-001", "admin", "系统管理员")));
    }

    @Test
    @DisplayName("解析Token Claims")
    void shouldParseTokenClaims() {
        String token = jwtTokenProvider.generateAccessToken("user-001", "admin", "系统管理员");
        Claims claims = jwtTokenProvider.parseToken(token);

        assertEquals("user-001", claims.getSubject());
        assertEquals("admin", claims.get("username"));
        assertEquals("系统管理员", claims.get("role"));
        assertEquals("access", claims.get("type"));
    }

    @Test
    @DisplayName("秘钥错误导致解析失败")
    void shouldFailOnWrongKey() {
        SecretKey wrongKey = generateKey();
        String wrongBase64 = Base64.getEncoder().encodeToString(wrongKey.getEncoded());
        JwtTokenProvider wrongProvider = new JwtTokenProvider(wrongBase64);
        String token = jwtTokenProvider.generateAccessToken("user-001", "admin", "系统管理员");

        assertFalse(wrongProvider.validateToken(token));
    }
}
