package com.pdm.log.entity;

import com.pdm.common.mybatis.BaseNamedEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审计日志实体类
 * 对应数据库表audit_log，存储系统操作审计相关信息
 *
 * @author 开发者
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_log")
public class AuditLog extends BaseNamedEntity {

    /**
     * 日志主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    @TableField("log_id")
    private Long logId;

    /**
     * 操作人唯一标识
     */
    @TableField("operator_uuid")
    private String operatorUuid;

    /**
     * 操作时间
     */
    @TableField("operation_time")
    private LocalDateTime operationTime;

    /**
     * 操作IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 操作类型（如：新增、修改、删除）
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 操作目标类型（如：用户、角色、菜单）
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 操作目标ID
     */
    @TableField("target_id")
    private String targetId;

    /**
     * 操作前数据（JSON格式）
     */
    @TableField("before_data")
    private String beforeData;

    /**
     * 操作后数据（JSON格式）
     */
    @TableField("after_data")
    private String afterData;
}