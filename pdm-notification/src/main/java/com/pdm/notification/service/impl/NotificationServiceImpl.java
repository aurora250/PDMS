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

/**
 * 通知服务业务逻辑实现类
 * 实现NotificationService接口，处理告警的创建、处理、查询等核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    /**
     * 告警信息数据访问层实例
     */
    private final AlertMapper alertMapper;

    /**
     * 创建告警信息
     * 若未指定是否处理，默认设置为未处理（0）
     * @param alert 待创建的告警信息实体
     * @return 创建成功的告警信息实体（包含自增主键）
     */
    @Override
    @Transactional
    public Alert createAlert(Alert alert) {
        if (alert.getIsHandled() == null) {
            alert.setIsHandled(0);
        }
        alertMapper.insert(alert);
        return alert;
    }

    /**
     * 标记告警为已处理
     * 更新告警的处理状态、处理人、处理时间
     * @param alertId 告警ID
     * @param handledBy 处理人
     * @return 更新后的告警信息实体
     * @throws RuntimeException 当告警ID不存在时抛出异常
     */
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

    /**
     * 获取未处理的告警列表
     * 按创建时间降序排列
     * @return 未处理的告警列表
     */
    @Override
    public List<Alert> getPendingAlerts() {
        return alertMapper.selectList(
                new LambdaQueryWrapper<Alert>().eq(Alert::getIsHandled, 0).orderByDesc(Alert::getCreateTime));
    }

    /**
     * 分页查询告警信息
     * 支持按告警类型、告警级别、处理状态筛选，结果按创建时间降序排列
     * @param alertType 告警类型（可选）
     * @param severity 告警级别（可选）
     * @param isHandled 处理状态（可选，0：未处理，1：已处理）
     * @param page 页码
     * @param size 每页条数
     * @return 分页后的告警信息结果
     */
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

    /**
     * 扫描过期/即将过期的许可证
     * 生产环境中会查询居住证许可证表，为即将过期或已过期的许可证创建告警
     */
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

    /**
     * 扫描逾期的来访计划
     * 生产环境中会查询重点人员来访计划表，为逾期的来访计划创建告警
     */
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