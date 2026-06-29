package com.pdm.resident.controller;

import com.pdm.common.core.result.Result;
import com.pdm.resident.service.StatisticsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * 统计数据控制器 — 提供省级人口分布、仪表盘概览等聚合查询
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /** 按省份聚合常住人口数据（用于中国地图着色） */
    @GetMapping("/province-population")
    public Result<List<Map<String, Object>>> getProvincePopulation() {
        return Result.success(statisticsService.getProvincePopulation());
    }

    /** 仪表盘概览数据 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardStats() {
        return Result.success(statisticsService.getDashboardStats());
    }

    /** 户籍迁移流向数据（省份间流动） */
    @GetMapping("/migration-flows")
    public Result<List<Map<String, Object>>> getMigrationFlows() {
        return Result.success(statisticsService.getMigrationFlows());
    }

    /** 某省份下各城市人口分布 */
    @GetMapping("/city-population")
    public Result<List<Map<String, Object>>> getCityPopulation(@RequestParam String province) {
        return Result.success(statisticsService.getCityPopulation(province));
    }
}
