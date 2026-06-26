package com.pdm.gateway.config;

import com.pdm.common.security.JwtTokenProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

import reactor.core.publisher.Mono;

/**
 * 网关 Bean 配置类。
 *
 * <p>负责初始化网关层所需的核心 Bean，包括 JWT 令牌提供器和限流键解析器。
 */
@Configuration
public class GatewayBeanConfig {

    /** JWT 签名密钥，从配置文件 {@code jwt.secret} 中注入 */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 创建 JWT 令牌提供器 Bean。
     *
     * <p>使用配置文件中的密钥初始化，用于在网关层解析和验证 JWT 令牌， 提取用户信息后通过请求头传递给下游微服务。
     *
     * @return JwtTokenProvider 实例
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret);
    }

    /**
     * 限流键解析器：按请求 IP 进行限流。
     *
     * <p>从请求的远程地址中提取 IP，作为 Redis 限流的键值， 实现基于来源 IP 的请求频率控制。
     *
     * @return KeyResolver 实例
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange ->
                Mono.just(
                        Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                                .getAddress()
                                .getHostAddress());
    }
}
