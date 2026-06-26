package com.pdm.log.service;

import com.pdm.common.dto.PageResult;
import com.pdm.log.entity.AuditLog;
import com.pdm.log.entity.LoginLog;
import java.time.LocalDateTime;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 日志服务接口
 * 定义审计日志、登录日志的记录、查询、导出等核心业务方法
 */
public interface LogService {

    /**
     * 记录审计日志
     *
     * @param auditLog 审计日志实体对象
     */
    void recordAuditLog(AuditLog auditLog);

    /**
     * 记录登录日志
     *
     * @param loginLog 登录日志实体对象
     */
    void recordLoginLog(LoginLog loginLog);

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
    PageResult<AuditLog> searchAuditLogs(LocalDateTime startTime, LocalDateTime endTime, String operatorUuid,
                                         String operationType, int page, int size);

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
    PageResult<LoginLog> searchLoginLogs(String userUuid, LocalDateTime startTime, LocalDateTime endTime,
                                         Integer isSuccess, int page, int size);

    /**
     * 导出审计日志
     *
     * @param startTime 操作开始时间
     * @param endTime 操作结束时间
     * @param operatorUuid 操作人唯一标识
     * @param operationType 操作类型
     * @param response HTTP响应对象，用于输出文件流
     */
    void exportAuditLogs(LocalDateTime startTime, LocalDateTime endTime, String operatorUuid, String operationType,
                         HttpServletResponse response);
}