package com.pdm.notification.service.impl;

import com.pdm.common.dto.PageResult;
import com.pdm.notification.entity.Alert;
import com.pdm.notification.mapper.AlertMapper;
import com.pdm.notification.service.NotificationService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final AlertMapper alertMapper;

    @Override
    @Transactional
    public Alert createAlert(Alert alert) {
        if (alert.getIsHandled() == null) {
            alert.setIsHandled(0);
        }
        alertMapper.insert(alert);
        return alert;
    }

    @Override
    @Transactional
    public Alert markHandled(Long alertId, String handledBy) {
        Alert alert = alertMapper.selectById(alertId);
        if (alert == null) {
            throw new RuntimeException("Alert not found: " + alertId);
        }
        alert.setIsHandled(1);
        alert.setHandledBy(handledBy);
        alert.setHandledAt(LocalDateTime.now());
        alertMapper.updateById(alert);
        return alert;
    }

    @Override
    public List<Alert> getPendingAlerts() {
        return alertMapper.selectList(
                new LambdaQueryWrapper<Alert>().eq(Alert::getIsHandled, 0).orderByDesc(Alert::getCreateTime));
    }

    @Override
    public PageResult<Alert> search(String alertType, String severity, Integer isHandled, int page, int size) {
        LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(alertType)) {
            wrapper.eq(Alert::getAlertType, alertType);
        }
        if (StringUtils.hasText(severity)) {
            wrapper.eq(Alert::getSeverity, severity);
        }
        if (isHandled != null) {
            wrapper.eq(Alert::getIsHandled, isHandled);
        }
        wrapper.orderByDesc(Alert::getCreateTime);

        IPage<Alert> result = alertMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    @Transactional
    public void scanPermitExpiry() {
        log.info("Scanning for expired permits...");
        // In production, this would query the resident permit table and create alerts
        // for permits nearing or past expiration. Placeholder implementation:
        // List<ResidentPermit> expiringPermits = permitMapper.selectExpiringPermits();
        // for (ResidentPermit permit : expiringPermits) { ... }
        log.info("Permit expiry scan completed.");
    }

    @Override
    @Transactional
    public void scanVisitOverdue() {
        log.info("Scanning for overdue visit plans...");
        // In production, this would query the key person visit plans and create alerts
        // for plans that are overdue. Placeholder implementation:
        // List<VisitPlan> overduePlans = visitPlanMapper.selectOverduePlans();
        // for (VisitPlan plan : overduePlans) { ... }
        log.info("Visit overdue scan completed.");
    }
}
