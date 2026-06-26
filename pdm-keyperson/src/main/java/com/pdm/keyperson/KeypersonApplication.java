package com.pdm.keyperson;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 重点人员管理服务启动类
 * 负责启动SpringBoot应用，开启服务注册发现，扫描MyBatis Mapper接口和指定包下的组件
 *
 * @author 开发者
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = { "com.pdm.keyperson", "com.pdm.common" })
@EnableDiscoveryClient
@MapperScan("com.pdm.keyperson.mapper")
public class KeypersonApplication {

    /**
     * 应用程序入口方法
     * 启动SpringBoot应用上下文
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(KeypersonApplication.class, args);
    }
}