package com.pdm.log.config;

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
 * 日志模块安全配置类
 * 配置JWT认证、CSRF防护、会话管理等安全规则
 *
 * @author 开发者
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class LogSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 配置安全过滤链
     *
     * @param http HttpSecurity对象，用于构建安全规则
     * @return 安全过滤链实例
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