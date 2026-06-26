package com.pdm.keyperson.entity;

import com.pdm.common.mybatis.BaseNamedEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 走访计划实体类
 * 对应数据库表：visit_plan，存储重点人员走访计划信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visit_plan")
public class VisitPlan extends BaseNamedEntity {

    /**
     * 走访计划ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    @TableField("plan_id")
    private Long planId;

    /**
     * 重点人员唯一标识
     */
    @TableField("key_person_uuid")
    private String keyPersonUuid;

    /**
     * 计划走访日期
     */
    @TableField("planned_date")
    private LocalDate plannedDate;

    /**
     * 实际走访日期
     */
    @TableField("actual_date")
    private LocalDate actualDate;

    /**
     * 走访类型（如：上门走访/电话回访等）
     */
    @TableField("visit_type")
    private String visitType;

    /**
     * 计划状态（待走访/已完成）
     */
    @TableField("status")
    private String status;

    /**
     * 指派民警编号
     */
    @TableField("assigned_police_no")
    private String assignedPoliceNo;

    /**
     * 是否已触发告警（0：未告警，1：已告警）
     */
    @TableField("is_alerted")
    private Integer isAlerted;
}