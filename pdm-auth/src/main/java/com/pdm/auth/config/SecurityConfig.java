package com.pdm.auth.config;

import com.pdm.common.security.JwtAuthenticationFilter;
import com.pdm.common.security.JwtTokenProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security 安全配置类。
 *
 * <p>
 * 负责配置整个认证授权模块的安全策略，包括：
 * </p>
 * <ul>
 * <li>禁用 CSRF 保护（适用于无状态 API）</li>
 * <li>设置会话管理为无状态模式</li>
 * <li>配置 URL 级别的权限控制</li>
 * <li>注册 JWT 认证过滤器</li>
 * <li>提供密码编码器 Bean</li>
 * </ul>
 *
 * @see JwtTokenProvider
 * @see JwtAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT 令牌提供器，用于生成、解析和验证 JWT 令牌 */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 配置安全过滤器链。
     *
     * <p>
     * 定义以下安全规则：
     * </p>
     * <ol>
     * <li>登录、注册、刷新令牌接口及 Swagger 文档接口对所有用户放开</li>
     * <li>用户管理相关接口需要"系统管理员"或"用户管理员"角色</li>
     * <li>权限组管理接口仅允许"系统管理员"角色访问</li>
     * <li>其余所有请求均需认证后访问</li>
     * </ol>
     *
     * @param http
     *            HttpSecurity 配置对象
     * @return 构建好的安全过滤器链
     * @throws Exception
     *             配置过程中可能抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll().requestMatchers("/api/auth/users/**").hasAnyRole("系统管理员", "用户管理员")
                        .requestMatchers("/api/auth/police/**").hasAnyRole("系统管理员", "用户管理员")
                        .requestMatchers("/api/auth/permission-groups/**").hasRole("系统管理员").anyRequest()
                        .authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 提供 BCrypt 密码编码器 Bean。
     *
     * <p>
     * 用于用户密码的加密存储和校验，BCrypt 是一种自包含盐值的强哈希算法。
     * </p>
     *
     * @return BCrypt 密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
