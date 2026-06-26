package com.pdm.log.config;

import com.pdm.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 日志模块Bean配置类
 * 负责初始化JWT相关Bean
 */
@Configuration
public class LogBeanConfig {

    /**
     * JWT加密密钥
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 构建JwtTokenProvider Bean
     *
     * @return JwtTokenProvider实例，用于JWT令牌的生成、解析和验证
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret);
    }
}