package com.pdm.notification.controller;

import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;
import com.pdm.notification.entity.Alert;
import com.pdm.notification.service.NotificationService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final NotificationService notificationService;

    @GetMapping("/pending")
    public Result<List<Alert>> getPendingAlerts() {
        return Result.success(notificationService.getPendingAlerts());
    }

    @PutMapping("/{id}/handle")
    public Result<Alert> markHandled(@PathVariable Long id, @RequestParam String handledBy) {
        return Result.success(notificationService.markHandled(id, handledBy));
    }

    @GetMapping("/search")
    public Result<PageResult<Alert>> search(@RequestParam(required = false) String alertType,
            @RequestParam(required = false) String severity, @RequestParam(required = false) Integer isHandled,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(notificationService.search(alertType, severity, isHandled, page, size));
    }
}
