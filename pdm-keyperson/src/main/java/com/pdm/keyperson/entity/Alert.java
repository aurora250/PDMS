package com.pdm.keyperson.entity;

import com.pdm.common.mybatis.BaseNamedEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 告警实体类
 * 对应数据库表：alert，存储重点人员相关告警信息
 *
 * @author 开发者
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert")
public class Alert extends BaseNamedEntity {

    /**
     * 告警ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    @TableField("alert_id")
    private Long alertId;

    /**
     * 告警类型（如：走访计划逾期/重点人员异动等）
     */
    @TableField("alert_type")
    private String alertType;

    /**
     * 目标类型（如：visit_plan/key_person）
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 目标ID（关联目标类型的主键）
     */
    @TableField("target_id")
    private String targetId;

    /**
     * 告警内容
     */
    @TableField("alert_content")
    private String alertContent;

    /**
     * 告警级别（如：高/中/低）
     */
    @TableField("severity")
    private String severity;

    /**
     * 是否已处理（0：未处理，1：已处理）
     */
    @TableField("is_handled")
    private Integer isHandled;

    /**
     * 处理人（民警编号）
     */
    @TableField("handled_by")
    private String handledBy;

    /**
     * 处理时间
     */
    @TableField("handled_at")
    private LocalDateTime handledAt;
}