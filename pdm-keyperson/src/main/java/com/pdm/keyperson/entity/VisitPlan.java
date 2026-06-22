package com.pdm.keyperson.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visit_plan")
public class VisitPlan extends BaseEntity {

    @TableField("plan_id")
    private String planId;

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
