package com.pdm.resident;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 常住人口服务启动类
 * 配置SpringBoot扫描包、服务注册发现、MyBatis Mapper扫描
 */
@SpringBootApplication(scanBasePackages = { "com.pdm.resident", "com.pdm.common" })
@EnableDiscoveryClient
@MapperScan("com.pdm.resident.mapper")
public class ResidentApplication {

    /**
     * 服务启动入口方法
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ResidentApplication.class, args);
    }
}