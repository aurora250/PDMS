package com.pdm.auth.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限组实体类，对应数据库 {@code permission_group} 表。
 *
 * <p>继承 {@link com.pdm.common.mybatis.BaseNamedEntity}，自动获得名称等通用字段。 权限组用于将一组权限打包授权给用户，实现基于角色的权限管理。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission_group")
public class PermissionGroup extends BaseNamedEntity {

    /** 权限组主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    @TableField("group_id")
    private Long groupId;

    /** 权限组名称 */
    @TableField("group_name")
    private String groupName;

    /** 权限组描述信息 */
    @TableField("description")
    private String description;

    /** 权限列表，以 JSON 数组格式存储的权限字符串集合 */
    @TableField("permissions")
    private String permissions;
}
