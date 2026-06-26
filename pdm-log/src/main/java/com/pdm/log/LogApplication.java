package com.pdm.log;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 日志服务启动类
 * 启动SpringBoot应用，开启服务发现、MyBatis Mapper扫描等功能
 *
 * @author 开发者
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = { "com.pdm.log", "com.pdm.common" })
@EnableDiscoveryClient
@MapperScan("com.pdm.log.mapper")
public class LogApplication {

    /**
     * 应用入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(LogApplication.class, args);
    }
}