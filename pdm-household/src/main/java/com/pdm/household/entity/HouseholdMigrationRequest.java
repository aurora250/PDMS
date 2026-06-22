package com.pdm.household.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDate;

@Data @EqualsAndHashCode(callSuper = true)
@TableName("household_migration_request")
public class HouseholdMigrationRequest extends BaseEntity {
    @TableField("rid") private Long rid;
    @TableField("handler_uuid") private String handlerUuid;
    @TableField("applicant_uuid") private String applicantUuid;
    @TableField("incoming_address") private String incomingAddress;
    @TableField("incoming_area_id") private Long incomingAreaId;
    @TableField("outgoing_address") private String outgoingAddress;
    @TableField("outgoing_area_id") private Long outgoingAreaId;
    @TableField("attachment") private String attachment;
    @TableField("business_type") private String businessType;
    @TableField("handle_date") private LocalDate handleDate;
    @TableField("handle_basis") private String handleBasis;
    @TableField("fee") private BigDecimal fee;
    @TableField("status") private String status;
    @TableField("reject_reason") private String rejectReason;
    @TableField("approval_permit_no") private String approvalPermitNo;
    @TableField("migration_permit_no") private String migrationPermitNo;
    @TableField("remark") private String remark;
}
