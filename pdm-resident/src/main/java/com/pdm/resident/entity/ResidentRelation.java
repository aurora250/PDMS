package com.pdm.resident.entity;

import com.pdm.common.mybatis.BaseNamedEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 常住人口亲属关系实体类
 * 对应数据库表 resident_relation
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_relation")
public class ResidentRelation extends BaseNamedEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    /**
     * 关联人员UUID
     */
    @TableField("relation_person_uuid")
    private String relationPersonUuid;

    /**
     * 父亲UUID
     */
    @TableField("father_uuid")
    private String fatherUuid;

    /**
     * 母亲UUID
     */
    @TableField("mother_uuid")
    private String motherUuid;

    /**
     * 配偶UUID
     */
    @TableField("spouse_uuid")
    private String spouseUuid;
}