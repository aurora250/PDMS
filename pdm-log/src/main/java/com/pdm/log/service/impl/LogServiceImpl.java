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

/**
 * 日志服务实现类
 * 实现LogService接口，提供审计日志、登录日志的记录、查询、导出等功能
 *
 * @author 开发者
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    /**
     * 审计日志Mapper
     */
    private final AuditLogMapper auditLogMapper;

    /**
     * 登录日志Mapper
     */
    private final LoginLogMapper loginLogMapper;

    /**
     * 记录审计日志
     * 若操作时间为空，则自动填充为当前时间
     *
     * @param auditLog 审计日志实体对象
     */
    @Override
    public void recordAuditLog(AuditLog auditLog) {
        if (auditLog.getOperationTime() == null) {
            auditLog.setOperationTime(LocalDateTime.now());
        }
        auditLogMapper.insert(auditLog);
    }

    /**
     * 记录登录日志
     * 若登录时间为空，则自动填充为当前时间
     *
     * @param loginLog 登录日志实体对象
     */
    @Override
    public void recordLoginLog(LoginLog loginLog) {
        if (loginLog.getLoginTime() == null) {
            loginLog.setLoginTime(LocalDateTime.now());
        }
        loginLogMapper.insert(loginLog);
    }

    /**
     * 分页查询审计日志
     *
     * @param startTime 操作开始时间
     * @param endTime 操作结束时间
     * @param operatorUuid 操作人唯一标识
     * @param operationType 操作类型
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果对象，包含审计日志列表及分页信息
     */
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

    /**
     * 分页查询登录日志
     *
     * @param userUuid 用户唯一标识
     * @param startTime 登录开始时间
     * @param endTime 登录结束时间
     * @param isSuccess 登录是否成功（1：成功，0：失败）
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果对象，包含登录日志列表及分页信息
     */
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

    /**
     * 导出审计日志
     * 将符合条件的审计日志导出为Excel格式（简化版为文本流）
     *
     * @param startTime 操作开始时间
     * @param endTime 操作结束时间
     * @param operatorUuid 操作人唯一标识
     * @param operationType 操作类型
     * @param response HTTP响应对象，用于输出文件流
     */
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