package com.pdm.household.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 行政区划区域实体类。
 *
 * <p>对应数据库表 {@code area}，用于存储省、市、区县等行政区划层级数据。 支持树形结构查询，通过 {@code parentId} 建立层级关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("area")
public class Area extends BaseNamedEntity {

    /** 区域主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    @TableField("area_id")
    private Long areaId;

    /** 行政区划编码 */
    @TableField("area_code")
    private String areaCode;

    /** 行政区划名称 */
    @TableField("area_name")
    private String areaName;

    /** 父级区域 ID，用于构建树形层级 */
    @TableField("parent_id")
    private Long parentId;

    /** 区域层级（省/市/区县等） */
    @TableField("area_level")
    private String areaLevel;
}
