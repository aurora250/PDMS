package com.pdm.common.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 认证过滤器。
 *
 * <p>继承 Spring 的 {@link OncePerRequestFilter}，确保每个请求仅执行一次。 从请求头 {@code Authorization: Bearer
 * <token>} 中提取 JWT 令牌，验证通过后：
 *
 * <ul>
 *   <li>将用户信息存入 {@link UserContextHolder}（基于 ThreadLocal）
 *   <li>设置 Spring Security 的认证上下文（包含用户角色）
 * </ul>
 *
 * 无论认证是否成功，请求结束后都会在 {@code finally} 块中清理用户上下文。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 令牌提供器 */
    private final JwtTokenProvider jwtTokenProvider;

    /** HTTP 请求头名称 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer 令牌前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 对每个请求进行 JWT 认证过滤。
     *
     * <p>提取令牌、验证有效性后设置安全上下文，请求处理完成后清理 ThreadLocal。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String userUuid = jwtTokenProvider.getUserUuid(token);
            String username = jwtTokenProvider.getUsername(token);
            String role = jwtTokenProvider.getRole(token);

            UserContextHolder.UserContext context =
                    new UserContextHolder.UserContext(
                            userUuid, username, role, request.getRemoteAddr());
            UserContextHolder.set(context);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userUuid,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
            authentication.setDetails(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    /**
     * 从 HTTP 请求头中提取 Bearer 令牌。
     *
     * @param request HTTP 请求
     * @return 提取的 JWT 令牌字符串，不存在或以非 Bearer 开头时返回 {@code null}
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
