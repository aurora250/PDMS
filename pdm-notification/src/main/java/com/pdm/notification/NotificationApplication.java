package com.pdm.notification;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 通知服务应用启动类
 * 启动SpringBoot应用，启用服务发现、定时任务、MyBatis Mapper扫描等核心功能
 */
@SpringBootApplication(scanBasePackages = { "com.pdm.notification", "com.pdm.common" })
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.pdm.notification.mapper")
public class NotificationApplication {
    /**
     * 应用入口方法
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}