package com.pdm.missingperson.config;

import com.pdm.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 失踪人员服务Bean配置类
 * 配置JWT令牌生成器等核心Bean
 */
@Configuration
public class MissingpersonBeanConfig {

    /**
     * JWT加密密钥，从配置文件读取
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 构建JwtTokenProvider Bean实例
     * 使用配置文件中的JWT密钥初始化令牌生成器
     *
     * @return JwtTokenProvider实例
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret);
    }
}