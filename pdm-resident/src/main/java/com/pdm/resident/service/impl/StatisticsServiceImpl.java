package com.pdm.resident.service.impl;

import com.pdm.resident.mapper.StatisticsMapper;
import com.pdm.resident.service.StatisticsService;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;

    @Override
    public List<Map<String, Object>> getProvincePopulation() {
        return statisticsMapper.getProvincePopulation();
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("residentCount", statisticsMapper.countResidents());
        stats.put("pendingAlerts", statisticsMapper.countPendingAlerts());
        return stats;
    }

    @Override
    public List<Map<String, Object>> getMigrationFlows() {
        return statisticsMapper.getMigrationFlows();
    }

    @Override
    public List<Map<String, Object>> getCityPopulation(String provinceName) {
        return statisticsMapper.getCityPopulation(provinceName);
    }
}
