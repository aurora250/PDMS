package com.pdm.keyperson;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = { "com.pdm.keyperson", "com.pdm.common" })
@EnableDiscoveryClient
@MapperScan("com.pdm.keyperson.mapper")
public class KeypersonApplication {
    public static void main(String[] args) {
        SpringApplication.run(KeypersonApplication.class, args);
    }
}
