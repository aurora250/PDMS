package com.pdm.log.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_log")
public class AuditLog extends BaseEntity {

    @TableField("operator_uuid")
    private String operatorUuid;

    @TableField("operation_time")
    private LocalDateTime operationTime;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("operation_type")
    private String operationType;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private String targetId;

    @TableField("before_data")
    private String beforeData;

    @TableField("after_data")
    private String afterData;
}
