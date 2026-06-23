package com.pdm.log.service.impl;

import com.pdm.common.dto.PageResult;
import com.pdm.log.entity.AuditLog;
import com.pdm.log.entity.LoginLog;
import com.pdm.log.mapper.AuditLogMapper;
import com.pdm.log.mapper.LoginLogMapper;
import com.pdm.log.service.LogService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final AuditLogMapper auditLogMapper;
    private final LoginLogMapper loginLogMapper;

    @Override
    public void recordAuditLog(AuditLog auditLog) {
        if (auditLog.getOperationTime() == null) {
            auditLog.setOperationTime(LocalDateTime.now());
        }
        auditLogMapper.insert(auditLog);
    }

    @Override
    public void recordLoginLog(LoginLog loginLog) {
        if (loginLog.getLoginTime() == null) {
            loginLog.setLoginTime(LocalDateTime.now());
        }
        loginLogMapper.insert(loginLog);
    }

    @Override
    public PageResult<AuditLog> searchAuditLogs(LocalDateTime startTime, LocalDateTime endTime, String operatorUuid,
            String operationType, int page, int size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(AuditLog::getOperationTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AuditLog::getOperationTime, endTime);
        }
        if (StringUtils.hasText(operatorUuid)) {
            wrapper.eq(AuditLog::getOperatorUuid, operatorUuid);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(AuditLog::getOperationType, operationType);
        }
        wrapper.orderByDesc(AuditLog::getOperationTime);

        IPage<AuditLog> result = auditLogMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public PageResult<LoginLog> searchLoginLogs(String userUuid, LocalDateTime startTime, LocalDateTime endTime,
            Integer isSuccess, int page, int size) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userUuid)) {
            wrapper.eq(LoginLog::getUserUuid, userUuid);
        }
        if (startTime != null) {
            wrapper.ge(LoginLog::getLoginTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(LoginLog::getLoginTime, endTime);
        }
        if (isSuccess != null) {
            wrapper.eq(LoginLog::getIsSuccess, isSuccess);
        }
        wrapper.orderByDesc(LoginLog::getLoginTime);

        IPage<LoginLog> result = loginLogMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public void exportAuditLogs(LocalDateTime startTime, LocalDateTime endTime, String operatorUuid,
            String operationType, HttpServletResponse response) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(AuditLog::getOperationTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AuditLog::getOperationTime, endTime);
        }
        if (StringUtils.hasText(operatorUuid)) {
            wrapper.eq(AuditLog::getOperatorUuid, operatorUuid);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(AuditLog::getOperationType, operationType);
        }
        wrapper.orderByDesc(AuditLog::getOperationTime);

        List<AuditLog> logs = auditLogMapper.selectList(wrapper);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=audit_logs_" + System.currentTimeMillis() + ".xlsx");

        try {
            // Simplified: write as CSV-like plain text to avoid EasyExcel dependency
            // requirement
            StringBuilder sb = new StringBuilder();
            sb.append("操作人,操作时间,IP地址,操作类型,目标类型,目标ID\n");
            for (AuditLog log : logs) {
                sb.append(log.getOperatorUuid()).append(",").append(log.getOperationTime()).append(",")
                        .append(log.getIpAddress()).append(",").append(log.getOperationType()).append(",")
                        .append(log.getTargetType()).append(",").append(log.getTargetId()).append("\n");
            }
            response.getWriter().write(sb.toString());
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("Failed to export audit logs", e);
        }
    }
}
