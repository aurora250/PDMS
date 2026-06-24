package com.pdm.household.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_permit")
public class ApprovalPermit extends BaseEntity {
    @TableField("permit_no")
    private String permitNo;

    @TableField("issue_date")
    private LocalDate issueDate;

    @TableField("expiry_date")
    private LocalDate expiryDate;

    @TableField("issuing_authority")
    private String issuingAuthority;

    @TableField("status")
    private String status;
}
