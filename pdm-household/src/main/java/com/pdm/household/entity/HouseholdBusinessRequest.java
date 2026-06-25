package com.pdm.household.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("household_business_request")
public class HouseholdBusinessRequest extends BaseNamedEntity {

    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    @TableField("handler_uuid")
    private String handlerUuid;

    @TableField("applicant_uuid")
    private String applicantUuid;

    @TableField("attachment")
    private String attachment;

    @TableField("business_type")
    private String businessType;

    @TableField("handle_date")
    private LocalDate handleDate;

    @TableField("handle_basis")
    private String handleBasis;

    @TableField("fee")
    private BigDecimal fee;

    @TableField("status")
    private String status;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("remark")
    private String remark;
}
