package com.pdm.resident.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_relation")
public class ResidentRelation extends BaseEntity {

    @TableField("rid")
    private Long rid;

    @TableField("relation_person_uuid")
    private String relationPersonUuid;

    @TableField("father_uuid")
    private String fatherUuid;

    @TableField("mother_uuid")
    private String motherUuid;

    @TableField("spouse_uuid")
    private String spouseUuid;
}
