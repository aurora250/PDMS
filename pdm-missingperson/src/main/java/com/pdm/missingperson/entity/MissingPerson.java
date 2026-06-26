package com.pdm.missingperson.entity;

import com.pdm.common.mybatis.BaseNamedEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 失踪人员实体类
 * 对应数据库表missing_person，继承基础命名实体类，包含失踪人员的核心信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("missing_person")
public class MissingPerson extends BaseNamedEntity {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    /**
     * 居民唯一标识
     */
    @TableField("resident_uuid")
    private String residentUuid;

    /**
     * 失踪日期
     */
    @TableField("missing_date")
    private LocalDate missingDate;

    /**
     * 失踪地点
     */
    @TableField("missing_place")
    private String missingPlace;

    /**
     * 照片地址
     */
    @TableField("photo")
    private String photo;

    /**
     * 外貌特征
     */
    @TableField("appearance")
    private String appearance;

    /**
     * 既往病史
     */
    @TableField("medical_history")
    private String medicalHistory;

    /**
     * 可能去向
     */
    @TableField("possible_way")
    private String possibleWay;

    /**
     * 联系电话
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 状态（如：失踪中、已经寻回）
     */
    @TableField("status")
    private String status;
}