package com.pdm.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 跨域配置类（Reactive）。
 *
 * <p>
 * 配置网关层的 CORS（跨域资源共享）策略，允许所有来源的请求， 支持常用 HTTP 方法，允许携带凭据以支持 JWT 认证。
 * </p>
 */
@Configuration
public class CorsConfig {

    /**
     * 创建 CORS Web 过滤器 Bean。
     *
     * <p>
     * 允许所有来源、常见 HTTP 方法（GET/POST/PUT/DELETE/OPTIONS）、所有请求头， 支持携带
     * Cookie/Authorization 凭据，预检请求缓存 3600 秒。
     * </p>
     *
     * @return CorsWebFilter 实例
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
