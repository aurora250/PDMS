package com.pdm.household.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("household_register")
public class HouseholdRegister extends BaseEntity {
    @TableField("household_book_no")
    private String householdBookNo;

    @TableField("householder_uuid")
    private String householderUuid;

    @TableField("establish_date")
    private LocalDate establishDate;

    @TableField("hukou_address")
    private String hukouAddress;

    @TableField("hukou_area_id")
    private Long hukouAreaId;

    @TableField("status")
    private String status;

    @TableField("member_uuid_list")
    private String memberUuidList;

    @TableField(exist = false)
    private String householderName;
}
