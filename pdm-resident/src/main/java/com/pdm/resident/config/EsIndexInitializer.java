package com.pdm.resident.config;

import com.pdm.resident.service.ResidentService;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 应用启动时自动检查并创建 Elasticsearch 索引，然后将 PostgreSQL
 * 中已有的居民数据同步到 ES。防止搜索请求因索引缺失或为空而降级到 DB。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexInitializer implements ApplicationRunner {

    private final ResidentService residentService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int count = residentService.reindexAllResidents();
            log.info("ES index bootstrap complete: {} residents indexed", count);
        } catch (Exception e) {
            log.warn("ES index initialization failed (ES may not be available yet): {}", e.getMessage());
        }
    }
}
