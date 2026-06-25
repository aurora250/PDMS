package com.pdm.floatingpopulation.entity;

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
@TableName("resident_registration")
public class ResidentRegistration extends BaseNamedEntity {

    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    @TableField("uuid")
    private String uuid;

    @TableField("original_address")
    private String originalAddress;

    @TableField("current_address")
    private String currentAddress;

    @TableField("area_id")
    private Long areaId;

    @TableField("address_type")
    private String addressType;

    @TableField("house_ownership")
    private String houseOwnership;

    @TableField("purpose")
    private String purpose;

    @TableField("expected_duration")
    private String expectedDuration;

    @TableField("work_unit")
    private String workUnit;

    @TableField("register_date")
    private LocalDate registerDate;
}
