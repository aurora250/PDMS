package com.pdm.floatingpopulation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 流动人口管理模块启动类。
 *
 * <p>负责流动人口登记、居住证管理、居住证续期、常住人口登记等功能的 Spring Boot 应用入口。 扫描 {@code com.pdm.floatingpopulation} 和
 * {@code com.pdm.common} 包下的组件， 并启用服务发现与 MyBatis Mapper 自动扫描。
 */
@SpringBootApplication(scanBasePackages = {"com.pdm.floatingpopulation", "com.pdm.common"})
@EnableDiscoveryClient
@MapperScan("com.pdm.floatingpopulation.mapper")
public class FloatingPopulationApplication {

    /**
     * 应用主入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FloatingPopulationApplication.class, args);
    }
}
