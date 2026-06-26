package com.pdm.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关模块启动类。
 *
 * <p>作为整个微服务系统的入口网关，负责请求路由、全局认证过滤、 跨域配置和限流控制。启用 Nacos 服务发现以实现动态路由。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    /**
     * 应用主入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
