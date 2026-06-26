package com.pdm.keyperson.config;

import com.pdm.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 重点人员服务Bean配置类
 * 配置自定义Bean实例，如JwtTokenProvider
 */
@Configuration
public class KeypersonBeanConfig {

    /**
     * JWT签名密钥（从配置文件读取）
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 构建JwtTokenProvider Bean
     * 用于JWT令牌的生成、验证、解析等操作
     *
     * @return JwtTokenProvider实例
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret);
    }
}