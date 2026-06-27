package com.pdm.household.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 行政区划实体 - 符合 GB/T 2260-2007 标准
 *
 * <p>
 * 中华人民共和国行政区划代码标准规定：
 * <ul>
 * <li>行政区划代码为6位数字</li>
 * <li>前两位表示省级行政区（省、直辖市、自治区、特别行政区）</li>
 * <li>第三、四位表示地级行政区（地级市、地区、自治州、盟等）</li>
 * <li>第五、六位表示县级行政区（市辖区、县级市、县、自治县等）</li>
 * </ul>
 *
 * @see <a href=
 *      "http://www.stats.gov.cn/sj/tjbz/tjyqhdmhcxhfdm/">国家统计局行政区划代码</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("area")
public class Area extends BaseNamedEntity {

    @TableId(type = IdType.AUTO)
    @TableField("area_id")
    private Long areaId;

    /**
     * 行政区划代码 - 符合 GB/T 2260-2007 标准 必须为6位数字（格式：^[0-9]{6}$）
     */
    @TableField("area_code")
    private String areaCode;

    @TableField("area_name")
    private String areaName;

    @TableField("parent_id")
    private Long parentId;

    /**
     * 行政区划级别：省/市/区县/街道/社区
     */
    @TableField("area_level")
    private String areaLevel;
}
