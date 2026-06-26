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

/**
 * 日志管理控制器
 * 提供审计日志、登录日志的查询、导出等接口
 *
 * @author 开发者
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    /**
     * 分页查询审计日志
     *
     * @param startTime 操作开始时间
     * @param endTime 操作结束时间
     * @param operatorUuid 操作人唯一标识
     * @param operationType 操作类型
     * @param page 页码，默认1
     * @param size 每页条数，默认20
     * @return 分页结果对象，包含审计日志列表及分页信息
     */
    @GetMapping("/audit")
    public Result<PageResult<AuditLog>> searchAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String operatorUuid, @RequestParam(required = false) String operationType,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(logService.searchAuditLogs(startTime, endTime, operatorUuid, operationType, page, size));
    }

    /**
     * 分页查询登录日志
     *
     * @param userUuid 用户唯一标识
     * @param startTime 登录开始时间
     * @param endTime 登录结束时间
     * @param isSuccess 登录是否成功（1：成功，0：失败）
     * @param page 页码，默认1
     * @param size 每页条数，默认20
     * @return 分页结果对象，包含登录日志列表及分页信息
     */
    @GetMapping("/login")
    public Result<PageResult<LoginLog>> searchLoginLogs(@RequestParam(required = false) String userUuid,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                        @RequestParam(required = false) Integer isSuccess, @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return Result.success(logService.searchLoginLogs(userUuid, startTime, endTime, isSuccess, page, size));
    }

    /**
     * 导出审计日志
     *
     * @param startTime 操作开始时间
     * @param endTime 操作结束时间
     * @param operatorUuid 操作人唯一标识
     * @param operationType 操作类型
     * @param response HTTP响应对象，用于输出文件流
     */
    @GetMapping("/export")
    public void exportAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String operatorUuid, @RequestParam(required = false) String operationType,
            HttpServletResponse response) {
        logService.exportAuditLogs(startTime, endTime, operatorUuid, operationType, response);
    }
}