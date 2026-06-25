package com.pdm.floatingpopulation.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_permit_renewal")
public class ResidentPermitRenewal extends BaseNamedEntity {

    @TableId(type = IdType.AUTO)
    @TableField("renewal_id")
    private Long renewalId;

    @TableField("permit_no")
    private String permitNo;

    @TableField("old_expiry_date")
    private LocalDate oldExpiryDate;

    @TableField("new_expiry_date")
    private LocalDate newExpiryDate;

    @TableField("renewal_date")
    private LocalDate renewalDate;

    @TableField("operator_uuid")
    private String operatorUuid;

    @TableField("remark")
    private String remark;
}
