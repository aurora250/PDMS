package com.pdm.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway 层安全配置（Reactive）
 *
 * <p>
 * 认证由 {@link com.pdm.gateway.filter.AuthGlobalFilter} 处理， 下游微服务各自负责细粒度授权，网关层仅禁用
 * CSRF 并放行所有请求。 CSRF 必须禁用：系统使用 JWT Bearer Token，无状态会话，不存在 CSRF 攻击面。
 *
 * @author aurora250
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                // 禁用 CSRF —— JWT Bearer Token 无状态架构下不需要
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // 网关层全部放行，Token 校验由 AuthGlobalFilter 完成
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                // 无状态会话
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable);

        return http.build();
    }
}
