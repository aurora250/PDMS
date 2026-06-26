package com.pdm.floatingpopulation.config;

import com.pdm.common.security.JwtTokenProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 流动人口模块 Bean 配置类。
 *
 * <p>
 * 负责初始化流动人口模块所需的核心 Bean，如 JWT 令牌提供器。
 * </p>
 */
@Configuration
public class FloatingPopulationBeanConfig {

    /** JWT 签名密钥，从配置文件 {@code jwt.secret} 中注入 */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 创建 JWT 令牌提供器 Bean。
     *
     * <p>
     * 使用配置文件中的密钥初始化，用于后续令牌的生成、解析和验证。
     * </p>
     *
     * @return JwtTokenProvider 实例
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret);
    }
}
