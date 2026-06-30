package com.pdm.resident;

import com.pdm.resident.mapper.StatisticsMapper;
import com.pdm.resident.service.impl.StatisticsServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("统计分析服务 — 单元测试")
class StatisticsServiceImplTest {

    @Mock
    private StatisticsMapper statisticsMapper;
    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @Nested
    @DisplayName("省份人口统计")
    class GetProvincePopulation {

        @Test
        @DisplayName("获取各省人口数据")
        void shouldGetProvincePopulation() {
            List<Map<String, Object>> mockResult = new ArrayList<>();
            Map<String, Object> gd = new HashMap<>();
            gd.put("province", "广东省");
            gd.put("count", 15000L);
            mockResult.add(gd);
            Map<String, Object> bj = new HashMap<>();
            bj.put("province", "北京市");
            bj.put("count", 8000L);
            mockResult.add(bj);

            when(statisticsMapper.getProvincePopulation()).thenReturn(mockResult);

            List<Map<String, Object>> result = statisticsService.getProvincePopulation();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("广东省", result.get(0).get("province"));
            assertEquals(15000L, result.get(0).get("count"));
        }

        @Test
        @DisplayName("无数据时返回空列表")
        void shouldReturnEmptyListWhenNoData() {
            when(statisticsMapper.getProvincePopulation()).thenReturn(List.of());

            List<Map<String, Object>> result = statisticsService.getProvincePopulation();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("仪表盘统计")
    class GetDashboardStats {

        @Test
        @DisplayName("获取仪表盘概览数据")
        void shouldGetDashboardStats() {
            when(statisticsMapper.countResidents()).thenReturn(15000L);
            when(statisticsMapper.countPendingAlerts()).thenReturn(5L);

            Map<String, Object> result = statisticsService.getDashboardStats();

            assertNotNull(result);
            assertEquals(15000L, result.get("residentCount"));
            assertEquals(5L, result.get("pendingAlerts"));
            verify(statisticsMapper).countResidents();
            verify(statisticsMapper).countPendingAlerts();
        }

        @Test
        @DisplayName("居民数为零时正常返回")
        void shouldHandleZeroCounts() {
            when(statisticsMapper.countResidents()).thenReturn(0L);
            when(statisticsMapper.countPendingAlerts()).thenReturn(0L);

            Map<String, Object> result = statisticsService.getDashboardStats();

            assertEquals(0L, result.get("residentCount"));
            assertEquals(0L, result.get("pendingAlerts"));
        }
    }

    @Nested
    @DisplayName("迁移流向")
    class GetMigrationFlows {

        @Test
        @DisplayName("获取迁移流向数据")
        void shouldGetMigrationFlows() {
            List<Map<String, Object>> mockResult = new ArrayList<>();
            Map<String, Object> f1 = new HashMap<>();
            f1.put("fromProvince", "广东省");
            f1.put("toProvince", "北京市");
            f1.put("count", 120L);
            mockResult.add(f1);
            Map<String, Object> f2 = new HashMap<>();
            f2.put("fromProvince", "四川省");
            f2.put("toProvince", "广东省");
            f2.put("count", 85L);
            mockResult.add(f2);

            when(statisticsMapper.getMigrationFlows()).thenReturn(mockResult);

            List<Map<String, Object>> result = statisticsService.getMigrationFlows();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(120L, result.get(0).get("count"));
        }

        @Test
        @DisplayName("无迁移数据时返回空列表")
        void shouldReturnEmptyListWhenNoFlows() {
            when(statisticsMapper.getMigrationFlows()).thenReturn(List.of());

            List<Map<String, Object>> result = statisticsService.getMigrationFlows();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("城市人口统计")
    class GetCityPopulation {

        @Test
        @DisplayName("获取某省份下各城市人口")
        void shouldGetCityPopulationByProvince() {
            List<Map<String, Object>> mockResult = new ArrayList<>();
            Map<String, Object> gz = new HashMap<>();
            gz.put("city", "广州市");
            gz.put("count", 5000L);
            mockResult.add(gz);
            Map<String, Object> sz = new HashMap<>();
            sz.put("city", "深圳市");
            sz.put("count", 4500L);
            mockResult.add(sz);

            when(statisticsMapper.getCityPopulation("广东省")).thenReturn(mockResult);

            List<Map<String, Object>> result = statisticsService.getCityPopulation("广东省");

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("广州市", result.get(0).get("city"));
            assertEquals(5000L, result.get(0).get("count"));
        }

        @Test
        @DisplayName("省份无下辖城市时返回空列表")
        void shouldReturnEmptyForNoCities() {
            when(statisticsMapper.getCityPopulation("西藏自治区")).thenReturn(List.of());

            List<Map<String, Object>> result = statisticsService.getCityPopulation("西藏自治区");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
