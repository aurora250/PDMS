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

/**
 * 户政业务申请实体类。
 *
 * <p>
 * 对应数据库表 {@code household_business_request}，用于记录户籍登记、注销、户主变更等业务的申请与审批信息。
 * 业务申请经四级审批流程：采集员录入 → 街道办初审 → 民警复核 → 市局审批。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("household_business_request")
public class HouseholdBusinessRequest extends BaseNamedEntity {

    /** 业务申请主键 ID（rid = request id），自增 */
    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    /** 处理人 UUID */
    @TableField("handler_uuid")
    private String handlerUuid;

    /** 申请人 UUID */
    @TableField("applicant_uuid")
    private String applicantUuid;

    /** 附件信息（JSON 或文件路径） */
    @TableField("attachment")
    private String attachment;

    /** 业务类型（登记/注销/户主变更等） */
    @TableField("business_type")
    private String businessType;

    /** 处理日期 */
    @TableField("handle_date")
    private LocalDate handleDate;

    /** 处理依据（法律或规定条款） */
    @TableField("handle_basis")
    private String handleBasis;

    /** 办理费用 */
    @TableField("fee")
    private BigDecimal fee;

    /** 审批状态（审批中/已通过/已驳回等） */
    @TableField("status")
    private String status;

    /** 驳回原因 */
    @TableField("reject_reason")
    private String rejectReason;

    /** 备注信息 */
    @TableField("remark")
    private String remark;
}
