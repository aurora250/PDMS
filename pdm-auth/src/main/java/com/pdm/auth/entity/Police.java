package com.pdm.auth.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("police")
public class Police extends BaseEntity {

    @TableField("police_number")
    private String policeNumber;

    @TableField("user_uuid")
    private String userUuid;

    @TableField("resident_uuid")
    private String residentUuid;

    @TableField("police_station")
    private String policeStation;

    @TableField("jurisdiction")
    private String jurisdiction;

    @NotNull(message = "辖区不能为空")
    @TableField("area_id")
    private Long areaId;

    @TableField("department")
    private String department;

    @TableField("police_rank")
    private String policeRank;

    @TableField("duty_status")
    private String dutyStatus;

    /** 关联居民姓名，非数据库字段 */
    @TableField(exist = false)
    private String residentName;
}
