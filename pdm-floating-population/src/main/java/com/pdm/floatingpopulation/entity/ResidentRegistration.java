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
 * 常住人口登记实体类。
 *
 * <p>对应数据库表 {@code resident_registration}，用于记录常住人口的居住登记信息， 包括原地址、现地址、住房性质、居住事由、工作单位等详细信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_registration")
public class ResidentRegistration extends BaseNamedEntity {

    /** 记录主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    /** 居民 UUID */
    @TableField("uuid")
    private String uuid;

    /** 原户籍地址 */
    @TableField("original_address")
    private String originalAddress;

    /** 现居住地址 */
    @TableField("current_address")
    private String currentAddress;

    /** 所属区域 ID */
    @TableField("area_id")
    private Long areaId;

    /** 地址类型（城镇/农村等） */
    @TableField("address_type")
    private String addressType;

    /** 房屋所有权性质（自有/租赁/借住等） */
    @TableField("house_ownership")
    private String houseOwnership;

    /** 居住事由（务工/经商/随迁/学习等） */
    @TableField("purpose")
    private String purpose;

    /** 预计居住时长 */
    @TableField("expected_duration")
    private String expectedDuration;

    /** 工作单位 */
    @TableField("work_unit")
    private String workUnit;

    /** 登记日期 */
    @TableField("register_date")
    private LocalDate registerDate;
}
