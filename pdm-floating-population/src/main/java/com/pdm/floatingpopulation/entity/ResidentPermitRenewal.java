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
 * 居住证续期记录实体类。
 *
 * <p>
 * 对应数据库表 {@code resident_permit_renewal}，用于记录居住证的续期操作， 包括续期前后有效期变化及操作人信息。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_permit_renewal")
public class ResidentPermitRenewal extends BaseNamedEntity {

    /** 续期记录主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    @TableField("renewal_id")
    private Long renewalId;

    /** 关联的居住证编号 */
    @TableField("permit_no")
    private String permitNo;

    /** 续期前有效期截止日期 */
    @TableField("old_expiry_date")
    private LocalDate oldExpiryDate;

    /** 续期后有效期截止日期 */
    @TableField("new_expiry_date")
    private LocalDate newExpiryDate;

    /** 续期办理日期 */
    @TableField("renewal_date")
    private LocalDate renewalDate;

    /** 操作人 UUID */
    @TableField("operator_uuid")
    private String operatorUuid;

    /** 备注信息 */
    @TableField("remark")
    private String remark;
}
