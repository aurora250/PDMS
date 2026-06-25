package com.pdm.missingperson.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("missing_person")
public class MissingPerson extends BaseNamedEntity {

    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    @TableField("resident_uuid")
    private String residentUuid;

    @TableField("missing_date")
    private LocalDate missingDate;

    @TableField("missing_place")
    private String missingPlace;

    @TableField("photo")
    private String photo;

    @TableField("appearance")
    private String appearance;

    @TableField("medical_history")
    private String medicalHistory;

    @TableField("possible_way")
    private String possibleWay;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("status")
    private String status;
}
