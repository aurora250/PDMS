package com.pdm.notification.service;

import com.pdm.common.dto.PageResult;
import com.pdm.notification.entity.Alert;
import java.util.List;

/**
 * 通知服务业务逻辑接口
 * 定义告警的创建、处理、查询及定时扫描相关业务方法
 */
public interface NotificationService {

    /**
     * 创建告警信息
     * @param alert 待创建的告警信息实体
     * @return 创建成功的告警信息实体
     */
    Alert createAlert(Alert alert);

    /**
     * 标记告警为已处理
     * @param alertId 告警ID
     * @param handledBy 处理人
     * @return 更新后的告警信息实体
     */
    Alert markHandled(Long alertId, String handledBy);

    /**
     * 获取未处理的告警列表
     * @return 未处理的告警列表
     */
    List<Alert> getPendingAlerts();

    /**
     * 分页查询告警信息
     * @param alertType 告警类型（可选）
     * @param severity 告警级别（可选）
     * @param isHandled 处理状态（可选）
     * @param page 页码
     * @param size 每页条数
     * @return 分页后的告警信息结果
     */
    PageResult<Alert> search(String alertType, String severity, Integer isHandled, int page, int size);

    /**
     * 扫描过期/即将过期的许可证并创建告警
     */
    void scanPermitExpiry();

    /**
     * 扫描逾期的来访计划并创建告警
     */
    void scanVisitOverdue();
}