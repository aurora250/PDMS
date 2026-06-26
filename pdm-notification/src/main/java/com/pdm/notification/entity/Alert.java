package com.pdm.notification.entity;

import com.pdm.common.mybatis.BaseNamedEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 告警信息实体类
 * 对应数据库中的alert表，存储系统各类告警信息（如许可证过期、来访逾期等）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert")
public class Alert extends BaseNamedEntity {

    /**
     * 告警ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    @TableField("alert_id")
    private Long alertId;

    /**
     * 告警类型（如许可证过期、来访逾期等）
     */
    @TableField("alert_type")
    private String alertType;

    /**
     * 目标类型（关联的业务对象类型）
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 目标ID（关联的业务对象ID）
     */
    @TableField("target_id")
    private String targetId;

    /**
     * 告警内容
     */
    @TableField("alert_content")
    private String alertContent;

    /**
     * 告警级别（如高、中、低）
     */
    @TableField("severity")
    private String severity;

    /**
     * 是否已处理（0：未处理，1：已处理）
     */
    @TableField("is_handled")
    private Integer isHandled;

    /**
     * 处理人
     */
    @TableField("handled_by")
    private String handledBy;

    /**
     * 处理时间
     */
    @TableField("handled_at")
    private LocalDateTime handledAt;
}