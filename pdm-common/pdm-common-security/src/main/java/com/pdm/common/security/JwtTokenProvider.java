package com.pdm.common.security;

import com.pdm.common.core.constant.BaseConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(String base64Secret, long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    public JwtTokenProvider(String base64Secret) {
        this(base64Secret, BaseConstants.JWT_EXPIRATION_MS);
    }

    public String generateAccessToken(String userUuid, String username, String role, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userUuid", userUuid);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("permissions", String.join(",", permissions != null ? permissions : Collections.emptyList()));
        claims.put("type", "access");

        return Jwts.builder().claims(claims).subject(userUuid).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs)).signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(String userUuid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return Jwts.builder().claims(claims).subject(userUuid).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs * 12)) // 24h
                .signWith(secretKey, Jwts.SIG.HS256).compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String getUserUuid(String token) {
        return parseToken(token).getSubject();
    }

    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 从JWT中提取权限列表.
     *
     * @return 权限字符串列表，空token或无permissions claim返回空列表
     */
    public List<String> getPermissions(String token) {
        Object perms = parseToken(token).get("permissions");
        if (perms == null || perms.toString().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(perms.toString().split(","));
    }

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
