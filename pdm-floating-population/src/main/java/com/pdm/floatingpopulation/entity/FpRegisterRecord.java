package com.pdm.floatingpopulation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fp_register_record")
public class FpRegisterRecord extends BaseEntity {

    @TableField("rid")
    private String rid;

    @TableField("residence_permit_no")
    private String residencePermitNo;

    @TableField("uuid")
    private String uuid;

    @TableField("agent_uuid")
    private String agentUuid;

    @TableField("attachment")
    private String attachment;

    @TableField("reviewer_uuid")
    private String reviewerUuid;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("register_date")
    private LocalDate registerDate;

    @TableField("review_date")
    private LocalDate reviewDate;
}
