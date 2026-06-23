package com.pdm.floatingpopulation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = { "com.pdm.floatingpopulation", "com.pdm.common" })
@EnableDiscoveryClient
@MapperScan("com.pdm.floatingpopulation.mapper")
public class FloatingPopulationApplication {
    public static void main(String[] args) {
        SpringApplication.run(FloatingPopulationApplication.class, args);
    }
}
