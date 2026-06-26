package com.pdm.floatingpopulation.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 居住证实体类。
 *
 * <p>
 * 对应数据库表 {@code resident_permit}，用于记录流动人口的居住证签发信息。 居住证是流动人口在居住地享受基本公共服务和便利的凭证。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_permit")
public class ResidentPermit extends BaseEntity {

    /** 居住证编号 */
    @TableField("permit_no")
    private String permitNo;

    /** 持证人 UUID */
    @TableField("uuid")
    private String uuid;

    /** 签发日期 */
    @TableField("issue_date")
    private LocalDate issueDate;

    /** 有效期截止日期 */
    @TableField("expiry_date")
    private LocalDate expiryDate;

    /** 居住证状态（有效/过期/作废等） */
    @TableField("status")
    private String status;
}
