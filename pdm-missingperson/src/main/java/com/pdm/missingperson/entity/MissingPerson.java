package com.pdm.missingperson.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("missing_person")
public class MissingPerson extends BaseEntity {

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
