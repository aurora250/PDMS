package com.pdm.log.service;

import com.pdm.common.dto.PageResult;
import com.pdm.log.entity.AuditLog;
import com.pdm.log.entity.LoginLog;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletResponse;

public interface LogService {

    void recordAuditLog(AuditLog auditLog);

    void recordLoginLog(LoginLog loginLog);

    PageResult<AuditLog> searchAuditLogs(LocalDateTime startTime, LocalDateTime endTime, String operatorUuid,
            String operationType, int page, int size);

    PageResult<LoginLog> searchLoginLogs(String userUuid, LocalDateTime startTime, LocalDateTime endTime,
            Integer isSuccess, int page, int size);

    void exportAuditLogs(LocalDateTime startTime, LocalDateTime endTime, String operatorUuid, String operationType,
            HttpServletResponse response);
}
