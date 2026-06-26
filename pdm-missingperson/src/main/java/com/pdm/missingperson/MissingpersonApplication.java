package com.pdm.missingperson;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 失踪人员服务应用启动类
 * 启动SpringBoot应用，开启Nacos服务发现，扫描指定包下的Mapper接口
 */
@SpringBootApplication(scanBasePackages = { "com.pdm.missingperson", "com.pdm.common" })
@EnableDiscoveryClient
@MapperScan("com.pdm.missingperson.mapper")
public class MissingpersonApplication {

    /**
     * 应用入口方法
     * 启动SpringBoot应用上下文
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MissingpersonApplication.class, args);
    }
}