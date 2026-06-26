package com.pdm.log.controller;

import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;
import com.pdm.log.entity.AuditLog;
import com.pdm.log.entity.LoginLog;
import com.pdm.log.service.LogService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping("/audit")
    public Result<PageResult<AuditLog>> searchAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime endTime,
            @RequestParam(required = false) String operatorUuid,
            @RequestParam(required = false) String operationType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(
                logService.searchAuditLogs(
                        startTime, endTime, operatorUuid, operationType, page, size));
    }

    @GetMapping("/login")
    public Result<PageResult<LoginLog>> searchLoginLogs(
            @RequestParam(required = false) String userUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime endTime,
            @RequestParam(required = false) Integer isSuccess,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(
                logService.searchLoginLogs(userUuid, startTime, endTime, isSuccess, page, size));
    }

    @GetMapping("/export")
    public void exportAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime endTime,
            @RequestParam(required = false) String operatorUuid,
            @RequestParam(required = false) String operationType,
            HttpServletResponse response) {
        logService.exportAuditLogs(startTime, endTime, operatorUuid, operationType, response);
    }
}
