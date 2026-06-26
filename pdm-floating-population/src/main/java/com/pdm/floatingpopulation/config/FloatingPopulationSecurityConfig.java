package com.pdm.floatingpopulation.config;

import com.pdm.common.security.JwtAuthenticationFilter;
import com.pdm.common.security.JwtTokenProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * 流动人口模块安全配置类。
 *
 * <p>
 * 配置 Spring Security 过滤器链：禁用 CSRF 防护、设置无状态会话、 对所有请求强制认证，并注册 JWT 认证过滤器。
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class FloatingPopulationSecurityConfig {

    /** JWT 令牌提供器，用于解析和验证请求中的 JWT 令牌 */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 配置安全过滤器链。
     *
     * <p>
     * 禁用 CSRF、设置为无状态会话以避免服务端存储会话信息、 要求所有请求必须携带有效 JWT 令牌进行认证。
     * </p>
     *
     * @param http
     *            HttpSecurity 配置对象
     * @return 构建好的 SecurityFilterChain
     * @throws Exception
     *             配置过程中可能抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
