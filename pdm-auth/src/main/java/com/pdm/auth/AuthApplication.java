package com.pdm.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证授权模块启动类。
 *
 * <p>负责用户认证、授权、警员管理、权限组管理等功能的 Spring Boot 应用入口。 扫描 {@code com.pdm.auth} 和 {@code com.pdm.common}
 * 包下的组件， 并启用服务发现与 MyBatis Mapper 自动扫描。
 */
@SpringBootApplication(scanBasePackages = {"com.pdm.auth", "com.pdm.common"})
@EnableDiscoveryClient
@MapperScan("com.pdm.auth.mapper")
public class AuthApplication {

    /**
     * 应用主入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
