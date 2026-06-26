package com.pdm.missingperson.config;

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
 * 失踪人员服务安全配置类
 * 配置JWT认证、CSRF防护、会话管理等安全策略
 */
@Configuration
@RequiredArgsConstructor
public class MissingpersonSecurityConfig {

    /**
     * JWT令牌提供器实例
     */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 配置安全过滤链
     * 1. 禁用CSRF防护
     * 2. 设置会话为无状态
     * 3. 所有请求需认证
     * 4. 添加JWT认证过滤器到用户名密码认证过滤器之前
     *
     * @param http HttpSecurity配置对象
     * @return 配置后的SecurityFilterChain实例
     * @throws Exception 配置过程中可能抛出的异常
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