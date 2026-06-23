package com.pdm.notification.config;

import com.pdm.notification.service.NotificationService;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ScheduledTaskConfig {

    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduledPermitExpiryScan() {
        log.info("Starting scheduled permit expiry scan...");
        notificationService.scanPermitExpiry();
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduledVisitOverdueScan() {
        log.info("Starting scheduled visit overdue scan...");
        notificationService.scanVisitOverdue();
    }
}
