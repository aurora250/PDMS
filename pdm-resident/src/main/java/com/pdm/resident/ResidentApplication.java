package com.pdm.resident;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = { "com.pdm.resident", "com.pdm.common" })
@EnableDiscoveryClient
@MapperScan("com.pdm.resident.mapper")
public class ResidentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResidentApplication.class, args);
    }
}
