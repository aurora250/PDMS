package com.pdm.keyperson.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert")
public class Alert extends BaseEntity {

    @TableField("alert_type")
    private String alertType;

    @TableField("target_uuid")
    private String targetUuid;

    @TableField("target_type")
    private String targetType;

    @TableField("alert_content")
    private String alertContent;

    @TableField("alert_time")
    private LocalDateTime alertTime;

    @TableField("is_read")
    private Integer isRead;

    @TableField("handler_uuid")
    private String handlerUuid;
}
