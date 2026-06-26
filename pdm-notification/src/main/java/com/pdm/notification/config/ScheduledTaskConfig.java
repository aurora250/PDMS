package com.pdm.notification.config;

import com.pdm.notification.service.NotificationService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 定时任务配置类
 * 配置系统各类定时扫描任务，如许可证过期扫描、来访逾期扫描
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ScheduledTaskConfig {

    /**
     * 通知服务业务逻辑层实例
     */
    private final NotificationService notificationService;

    /**
     * 定时扫描许可证过期任务
     * 执行时机：每天8点执行
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduledPermitExpiryScan() {
        log.info("Starting scheduled permit expiry scan...");
        notificationService.scanPermitExpiry();
    }

    /**
     * 定时扫描来访逾期任务
     * 执行时机：每天8点执行
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduledVisitOverdueScan() {
        log.info("Starting scheduled visit overdue scan...");
        notificationService.scanVisitOverdue();
    }
}