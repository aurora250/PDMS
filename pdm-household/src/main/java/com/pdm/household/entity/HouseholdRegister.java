package com.pdm.household.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 户口本登记实体类。
 *
 * <p>
 * 对应数据库表 {@code household_register}，用于记录户口本的核心信息， 包括户口本编号、户主信息、户籍地址及家庭成员列表。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("household_register")
public class HouseholdRegister extends BaseEntity {

    /** 户口本编号 */
    @TableField("household_book_no")
    private String householdBookNo;

    /** 户主 UUID */
    @TableField("householder_uuid")
    private String householderUuid;

    /** 户口本建立日期 */
    @TableField("establish_date")
    private LocalDate establishDate;

    /** 户籍地址 */
    @TableField("hukou_address")
    private String hukouAddress;

    /** 户籍所在区域 ID */
    @TableField("hukou_area_id")
    private Long hukouAreaId;

    /** 户口本状态（审批中/有效/注销等） */
    @TableField("status")
    private String status;

    /** 家庭成员 UUID 列表（JSON 数组格式） */
    @TableField("member_uuid_list")
    private String memberUuidList;
}
