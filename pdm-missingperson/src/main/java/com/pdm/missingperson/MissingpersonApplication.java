package com.pdm.missingperson;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = { "com.pdm.missingperson", "com.pdm.common" })
@EnableDiscoveryClient
@MapperScan("com.pdm.missingperson.mapper")
public class MissingpersonApplication {
    public static void main(String[] args) {
        SpringApplication.run(MissingpersonApplication.class, args);
    }
}
