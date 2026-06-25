package com.pdm.keyperson.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visit_plan")
public class VisitPlan extends BaseNamedEntity {

    @TableId(type = IdType.AUTO)
    @TableField("plan_id")
    private Long planId;

    @TableField("key_person_uuid")
    private String keyPersonUuid;

    @TableField("planned_date")
    private LocalDate plannedDate;

    @TableField("actual_date")
    private LocalDate actualDate;

    @TableField("visit_type")
    private String visitType;

    @TableField("status")
    private String status;

    @TableField("assigned_police_no")
    private String assignedPoliceNo;

    @TableField("is_alerted")
    private Integer isAlerted;
}
