package com.pdm.household.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 准迁证实体类。
 *
 * <p>对应数据库表 {@code approval_permit}，用于记录户口迁移准迁证的签发信息。 准迁证是户口迁移流程中的重要证件，由迁入地公安机关签发。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_permit")
public class ApprovalPermit extends BaseEntity {

    /** 准迁证编号 */
    @TableField("permit_no")
    private String permitNo;

    /** 签发日期 */
    @TableField("issue_date")
    private LocalDate issueDate;

    /** 有效期截止日期 */
    @TableField("expiry_date")
    private LocalDate expiryDate;

    /** 签发机关 */
    @TableField("issuing_authority")
    private String issuingAuthority;

    /** 证件状态（有效/作废等） */
    @TableField("status")
    private String status;
}
