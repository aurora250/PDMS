package com.pdm.log.service;

import com.pdm.common.dto.PageResult;
import com.pdm.log.entity.AuditLog;
import com.pdm.log.entity.LoginLog;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

public interface LogService {

    void recordAuditLog(AuditLog auditLog);

    void recordLoginLog(LoginLog loginLog);

    PageResult<AuditLog> searchAuditLogs(LocalDateTime startTime, LocalDateTime endTime,
                                          String operatorUuid, String operationType,
                                          int page, int size);

    PageResult<LoginLog> searchLoginLogs(String userUuid, LocalDateTime startTime,
                                          LocalDateTime endTime, Integer isSuccess,
                                          int page, int size);

    void exportAuditLogs(LocalDateTime startTime, LocalDateTime endTime,
                         String operatorUuid, String operationType,
                         HttpServletResponse response);
}
