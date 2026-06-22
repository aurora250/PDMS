package com.pdm.floatingpopulation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_permit_renewal")
public class ResidentPermitRenewal extends BaseEntity {

    @TableField("renewal_id")
    private String renewalId;

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
