package com.pdm.auth.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 警员实体类，对应数据库 {@code police} 表。
 *
 * <p>
 * 继承 {@link BaseEntity}，自动获得 ID、创建时间、更新时间、逻辑删除等通用字段。 记录警员的基本信息、所属单位、警衔以及当前值班状态。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("police")
public class Police extends BaseEntity {

    /** 警号，警员的唯一业务标识 */
    @TableField("police_number")
    private String policeNumber;

    /** 关联的系统用户 UUID */
    @TableField("user_uuid")
    private String userUuid;

    /** 关联的居民实名认证 UUID */
    @TableField("resident_uuid")
    private String residentUuid;

    /** 所属派出所 */
    @TableField("police_station")
    private String policeStation;

    /** 管辖区域描述 */
    @TableField("jurisdiction")
    private String jurisdiction;

    /** 管辖区域 ID */
    @TableField("area_id")
    private Long areaId;

    /** 所属部门 */
    @TableField("department")
    private String department;

    /** 警衔等级 */
    @TableField("police_rank")
    private String policeRank;

    /** 值班状态（在岗 / 调岗 / 离职） */
    @TableField("duty_status")
    private String dutyStatus;
}
