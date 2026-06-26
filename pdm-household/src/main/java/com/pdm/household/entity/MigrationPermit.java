package com.pdm.household.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 迁移证实体类。
 *
 * <p>
 * 对应数据库表 {@code migration_permit}，用于记录户口迁移证的签发信息。 迁移证由迁出地公安机关签发，是户口迁移流程中的关键证件。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("migration_permit")
public class MigrationPermit extends BaseEntity {

    /** 迁移证编号 */
    @TableField("permit_no")
    private String permitNo;

    /** 签发日期 */
    @TableField("issue_date")
    private LocalDate issueDate;

    /** 有效期截止日期 */
    @TableField("expiry_date")
    private LocalDate expiryDate;

    /** 迁出地派出所 */
    @TableField("outgoing_police_station")
    private String outgoingPoliceStation;

    /** 迁移证状态（有效/作废等） */
    @TableField("status")
    private String status;
}
