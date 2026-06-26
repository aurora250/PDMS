package com.pdm.floatingpopulation.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流动人口登记记录实体类。
 *
 * <p>对应数据库表 {@code fp_register_record}，用于记录流动人口的登记信息， 包括关联的居住证编号、经办人、审核人及审核状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fp_register_record")
public class FpRegisterRecord extends BaseNamedEntity {

    /** 记录主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    /** 关联的居住证编号 */
    @TableField("residence_permit_no")
    private String residencePermitNo;

    /** 流动人口 UUID */
    @TableField("uuid")
    private String uuid;

    /** 经办人 UUID */
    @TableField("agent_uuid")
    private String agentUuid;

    /** 附件信息（JSON 或文件路径） */
    @TableField("attachment")
    private String attachment;

    /** 审核人 UUID */
    @TableField("reviewer_uuid")
    private String reviewerUuid;

    /** 驳回原因 */
    @TableField("reject_reason")
    private String rejectReason;

    /** 登记日期 */
    @TableField("register_date")
    private LocalDate registerDate;

    /** 审核日期 */
    @TableField("review_date")
    private LocalDate reviewDate;
}
