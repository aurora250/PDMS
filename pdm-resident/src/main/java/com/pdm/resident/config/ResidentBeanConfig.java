package com.pdm.resident.config;

import com.pdm.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 常住人口模块Bean配置类
 * 用于注册模块所需的自定义Bean
 */
@Configuration
public class ResidentBeanConfig {

    /**
     * JWT密钥
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 构建JWT令牌提供者Bean
     * @return JwtTokenProvider JWT令牌处理实例
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret);
    }
}