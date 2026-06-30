package com.pdm.resident.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;

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

    @ExcelIgnore
    @TableField("uuid")
    private String uuid;

    @ExcelProperty("姓名")
    @TableField("name")
    private String name;

    @ExcelProperty("曾用名")
    @TableField("former_name")
    private String formerName;

    @ExcelProperty("性别")
    @TableField("gender")
    private String gender;

    @ExcelProperty("身份证号")
    @TableField("id_card_no")
    private String idCardNo;

    @ExcelProperty("民族")
    @TableField("nation")
    private String nation;

    @ExcelIgnore
    @TableField("nation_code")
    private String nationCode;

    @ExcelProperty("出生日期")
    @DateTimeFormat("yyyy-MM-dd")
    @TableField("birth_date")
    private LocalDate birthDate;

    @ExcelProperty("学历")
    @TableField("education_level")
    private String educationLevel;

    @ExcelIgnore
    @TableField("education_code")
    private String educationCode;

    @ExcelProperty("血型")
    @TableField("blood_type")
    private String bloodType;

    @ExcelProperty("婚姻状况")
    @TableField("marital_status")
    private String maritalStatus;

    @ExcelProperty("职业")
    @TableField("occupation")
    private String occupation;

    @ExcelProperty("电话")
    @TableField("phone")
    private String phone;

    @ExcelIgnore
    @TableField("photo")
    private String photo;

    @ExcelProperty("居住地址")
    @TableField("residence")
    private String residence;

    @ExcelIgnore
    @TableField("area_id")
    private Long areaId;

    @ExcelProperty("户口类型")
    @TableField("household_type")
    private String householdType;

    @ExcelProperty("户籍状态")
    @TableField("household_status")
    private String householdStatus;

    @ExcelProperty("户籍地址")
    @TableField("household_address")
    private String householdAddress;

    @ExcelIgnore
    @TableField("household_area_id")
    private Long householdAreaId;
}
