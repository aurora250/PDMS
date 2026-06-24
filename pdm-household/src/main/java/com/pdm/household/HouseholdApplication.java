package com.pdm.household;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = { "com.pdm.household", "com.pdm.common" })
@EnableDiscoveryClient
@MapperScan("com.pdm.household.mapper")
public class HouseholdApplication {
    public static void main(String[] args) {
        SpringApplication.run(HouseholdApplication.class, args);
    }
}
