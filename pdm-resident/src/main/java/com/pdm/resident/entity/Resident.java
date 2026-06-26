package com.pdm.resident.entity;

import com.pdm.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 常住人口基础信息实体类
 * 对应数据库表 resident
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident")
public class Resident extends BaseEntity {

    /**
     * 人员唯一标识
     */
    @TableField("uuid")
    private String uuid;

    /**
     * 姓名
     */
    @TableField("name")
    private String name;

    /**
     * 曾用名
     */
    @TableField("former_name")
    private String formerName;

    /**
     * 性别
     */
    @TableField("gender")
    private String gender;

    /**
     * 身份证号
     */
    @TableField("id_card_no")
    private String idCardNo;

    /**
     * 民族
     */
    @TableField("nation")
    private String nation;

    /**
     * 出生日期
     */
    @TableField("birth_date")
    private LocalDate birthDate;

    /**
     * 文化程度
     */
    @TableField("education_level")
    private String educationLevel;

    /**
     * 血型
     */
    @TableField("blood_type")
    private String bloodType;

    /**
     * 婚姻状况
     */
    @TableField("marital_status")
    private String maritalStatus;

    /**
     * 职业
     */
    @TableField("occupation")
    private String occupation;

    /**
     * 联系电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 照片地址
     */
    @TableField("photo")
    private String photo;

    /**
     * 居住地
     */
    @TableField("residence")
    private String residence;

    /**
     * 所属区域ID
     */
    @TableField("area_id")
    private Long areaId;

    /**
     * 户口类型
     */
    @TableField("household_type")
    private String householdType;

    /**
     * 户口状态
     */
    @TableField("household_status")
    private String householdStatus;

    /**
     * 户籍地址
     */
    @TableField("household_address")
    private String householdAddress;

    /**
     * 户籍所属区域ID
     */
    @TableField("household_area_id")
    private Long householdAreaId;
}