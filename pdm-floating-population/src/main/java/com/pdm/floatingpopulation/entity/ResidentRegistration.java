package com.pdm.floatingpopulation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_registration")
public class ResidentRegistration extends BaseEntity {

    @TableField("rid")
    private String rid;

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
