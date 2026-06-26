package com.pdm.household;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 户政管理模块启动类。
 *
 * <p>
 * 负责户口本管理、户政业务审批、户口迁移管理、证件签发等功能的 Spring Boot 应用入口。 扫描 {@code com.pdm.household}
 * 和 {@code com.pdm.common} 包下的组件， 并启用服务发现与 MyBatis Mapper 自动扫描。
 * </p>
 */
@SpringBootApplication(scanBasePackages = { "com.pdm.household", "com.pdm.common" })
@EnableDiscoveryClient
@MapperScan("com.pdm.household.mapper")
public class HouseholdApplication {

    /**
     * 应用主入口。
     *
     * @param args
     *            命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(HouseholdApplication.class, args);
    }
}
