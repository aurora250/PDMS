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
 * 户口迁移申请实体类。
 *
 * <p>对应数据库表 {@code household_migration_request}，用于记录户口迁移的申请与审批信息。 包括迁入地和迁出地的详细地址、关联的准迁证和迁移证编号。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("household_migration_request")
public class HouseholdMigrationRequest extends BaseNamedEntity {

    /** 迁移申请主键 ID（rid = request id），自增 */
    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    /** 处理人 UUID */
    @TableField("handler_uuid")
    private String handlerUuid;

    /** 申请人 UUID */
    @TableField("applicant_uuid")
    private String applicantUuid;

    /** 迁入地详细地址 */
    @TableField("incoming_address")
    private String incomingAddress;

    /** 迁入地区域 ID */
    @TableField("incoming_area_id")
    private Long incomingAreaId;

    /** 迁出地详细地址 */
    @TableField("outgoing_address")
    private String outgoingAddress;

    /** 迁出地区域 ID */
    @TableField("outgoing_area_id")
    private Long outgoingAreaId;

    /** 附件信息（JSON 或文件路径） */
    @TableField("attachment")
    private String attachment;

    /** 业务类型 */
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

    /** 审批状态（准迁证审批中/已通过/已驳回等） */
    @TableField("status")
    private String status;

    /** 驳回原因 */
    @TableField("reject_reason")
    private String rejectReason;

    /** 关联的准迁证编号 */
    @TableField("approval_permit_no")
    private String approvalPermitNo;

    /** 关联的迁移证编号 */
    @TableField("migration_permit_no")
    private String migrationPermitNo;

    /** 备注信息 */
    @TableField("remark")
    private String remark;
}
