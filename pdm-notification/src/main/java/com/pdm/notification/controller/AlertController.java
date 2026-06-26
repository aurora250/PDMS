package com.pdm.notification.controller;

import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;
import com.pdm.notification.entity.Alert;
import com.pdm.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * 告警信息控制器
 * 提供告警相关的RESTful接口，包括查询未处理告警、标记告警已处理、分页查询告警等
 */
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    /**
     * 通知服务业务逻辑层实例
     */
    private final NotificationService notificationService;

    /**
     * 获取未处理的告警列表
     * @return 响应结果，包含未处理的告警列表
     */
    @GetMapping("/pending")
    public Result<List<Alert>> getPendingAlerts() {
        return Result.success(notificationService.getPendingAlerts());
    }

    /**
     * 标记指定ID的告警为已处理
     * @param id 告警ID
     * @param handledBy 处理人
     * @return 响应结果，包含更新后的告警信息实体
     */
    @PutMapping("/{id}/handle")
    public Result<Alert> markHandled(@PathVariable Long id, @RequestParam String handledBy) {
        return Result.success(notificationService.markHandled(id, handledBy));
    }

    /**
     * 分页查询告警信息
     * @param alertType 告警类型（可选）
     * @param severity 告警级别（可选）
     * @param isHandled 处理状态（可选）
     * @param page 页码，默认1
     * @param size 每页条数，默认20
     * @return 响应结果，包含分页后的告警信息
     */
    @GetMapping("/search")
    public Result<PageResult<Alert>> search(@RequestParam(required = false) String alertType,
                                            @RequestParam(required = false) String severity, @RequestParam(required = false) Integer isHandled,
                                            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(notificationService.search(alertType, severity, isHandled, page, size));
    }
}