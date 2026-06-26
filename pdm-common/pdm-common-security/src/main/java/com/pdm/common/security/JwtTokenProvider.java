package com.pdm.common.security;

import com.pdm.common.core.constant.BaseConstants;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 令牌提供器。
 *
 * <p>负责 JWT 访问令牌和刷新令牌的生成、解析、验证和信息提取。 使用 HMAC-SHA256 签名算法，密钥由 Base64 编码的密钥字符串初始化。 访问令牌包含用户
 * UUID、用户名、角色和令牌类型等声明（claims）。
 */
@Slf4j
public class JwtTokenProvider {

    /** HMAC-SHA256 签名密钥 */
    private final SecretKey secretKey;

    /** 访问令牌过期时间（毫秒） */
    private final long expirationMs;

    /**
     * 使用自定义密钥和过期时间构造。
     *
     * @param base64Secret Base64 编码的密钥字符串
     * @param expirationMs 令牌过期时间（毫秒）
     */
    public JwtTokenProvider(String base64Secret, long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    /**
     * 使用自定义密钥和默认过期时间（2 小时）构造。
     *
     * @param base64Secret Base64 编码的密钥字符串
     */
    public JwtTokenProvider(String base64Secret) {
        this(base64Secret, BaseConstants.JWT_EXPIRATION_MS);
    }

    /**
     * 生成 JWT 访问令牌。
     *
     * <p>令牌中包含用户 UUID（作为 subject）、用户名、角色和类型声明， 过期时间由 {@link #expirationMs} 指定。
     *
     * @param userUuid 用户 UUID
     * @param username 用户名
     * @param role 用户角色
     * @return 签名的 JWT 访问令牌字符串
     */
    public String generateAccessToken(String userUuid, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userUuid", userUuid);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("type", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(userUuid)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成 JWT 刷新令牌。
     *
     * <p>刷新令牌有效期为访问令牌的 12 倍（默认 24 小时），仅包含类型声明和 subject， 不含用户详细信息，安全性更高。
     *
     * @param userUuid 用户 UUID
     * @return 签名的 JWT 刷新令牌字符串
     */
    public String generateRefreshToken(String userUuid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(userUuid)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs * 12)) // 24h
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析 JWT 令牌，提取其中的声明（claims）。
     *
     * <p>解析过程中自动进行签名验证和过期检查。
     *
     * @param token JWT 令牌字符串
     * @return 令牌中存储的声明
     * @throws JwtException 令牌解析或验证失败时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    /**
     * 验证 JWT 令牌是否有效。
     *
     * <p>包含签名验证和过期检查，验证失败时记录 debug 日志。
     *
     * @param token JWT 令牌字符串
     * @return true 表示令牌有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从令牌中获取用户 UUID（subject）。
     *
     * @param token JWT 令牌字符串
     * @return 用户 UUID
     */
    public String getUserUuid(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从令牌中获取用户名。
     *
     * @param token JWT 令牌字符串
     * @return 用户名
     */
    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    /**
     * 从令牌中获取用户角色。
     *
     * @param token JWT 令牌字符串
     * @return 用户角色
     */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 判断令牌是否即将过期（剩余有效期小于刷新阈值）。
     *
     * <p>当令牌解析失败时（如已过期），结果为 true，触发刷新逻辑。
     *
     * @param token JWT 令牌字符串
     * @return true 表示令牌即将过期或已过期，应触发刷新
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = parseToken(token);
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            return remaining < BaseConstants.JWT_REFRESH_THRESHOLD_MS;
        } catch (Exception e) {
            return true;
        }
    }
}
