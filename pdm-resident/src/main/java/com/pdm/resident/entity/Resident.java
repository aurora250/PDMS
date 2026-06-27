package com.pdm.resident.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident")
public class Resident extends BaseEntity {

    @TableField("uuid")
    private String uuid;

    @TableField("name")
    private String name;

    @TableField("former_name")
    private String formerName;

    @TableField("gender")
    private String gender;

    @TableField("id_card_no")
    private String idCardNo;

    @TableField("nation")
    private String nation;

    /**
     * 民族代码 符合 GB 3304-1991 中国各民族名称的罗马字母拼写法和代码
     */
    @TableField("nation_code")
    private String nationCode;

    @TableField("birth_date")
    private LocalDate birthDate;

    @TableField("education_level")
    private String educationLevel;

    /**
     * 学历代码 符合 GB/T 4658-2006 学历代码
     */
    @TableField("education_code")
    private String educationCode;

    @TableField("blood_type")
    private String bloodType;

    @TableField("marital_status")
    private String maritalStatus;

    @TableField("occupation")
    private String occupation;

    @TableField("phone")
    private String phone;

    @TableField("photo")
    private String photo;

    @TableField("residence")
    private String residence;

    @TableField("area_id")
    private Long areaId;

    @TableField("household_type")
    private String householdType;

    @TableField("household_status")
    private String householdStatus;

    @TableField("household_address")
    private String householdAddress;

    @TableField("household_area_id")
    private Long householdAreaId;
}
