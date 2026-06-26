package com.pdm.gateway.config;

import com.pdm.common.security.JwtTokenProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

import reactor.core.publisher.Mono;

@Configuration
public class GatewayBeanConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret);
    }

    /** 限流Key解析: 按请求IP限流 */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono
                .just(Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress());
    }
}
