package com.pdm.floatingpopulation.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_permit")
public class ResidentPermit extends BaseEntity {

    @TableField("permit_no")
    private String permitNo;

    @TableField("uuid")
    private String uuid;

    @TableField("issue_date")
    private LocalDate issueDate;

    @TableField("expiry_date")
    private LocalDate expiryDate;

    @TableField("status")
    private String status;
}
