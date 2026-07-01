package com.pdm.log.config;

import com.pdm.common.security.JwtTokenProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogBeanConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    @ConditionalOnMissingBean(JwtTokenProvider.class)
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret);
    }
}
