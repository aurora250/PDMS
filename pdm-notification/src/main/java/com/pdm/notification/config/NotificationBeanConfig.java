package com.pdm.notification.config;

import com.pdm.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通知服务Bean配置类
 * 配置JWT令牌工具类等核心Bean
 */
@Configuration
public class NotificationBeanConfig {

    /**
     * JWT签名密钥，从配置文件读取
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 创建JWT令牌生成与验证工具类Bean
     * @return JwtTokenProvider实例
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret);
    }
}