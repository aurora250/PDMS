package com.pdm.notification.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert")
public class Alert extends BaseNamedEntity {

    @TableId(type = IdType.AUTO)
    @TableField("alert_id")
    private Long alertId;

    @TableField("alert_type")
    private String alertType;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private String targetId;

    @TableField("alert_content")
    private String alertContent;

    @TableField("severity")
    private String severity;

    @TableField("is_handled")
    private Integer isHandled;

    @TableField("handled_by")
    private String handledBy;

    @TableField("handled_at")
    private LocalDateTime handledAt;
}
