package com.pdm.resident.service;

import java.util.List;
import java.util.Map;

public interface StatisticsService {

    /** 按省份聚合人口数据（中国地图用） */
    List<Map<String, Object>> getProvincePopulation();

    /** 仪表盘概览数据 */
    Map<String, Object> getDashboardStats();

    /** 户籍迁移流向数据 */
    List<Map<String, Object>> getMigrationFlows();

    /** 某省份下各城市人口统计 */
    List<Map<String, Object>> getCityPopulation(String provinceName);
}
