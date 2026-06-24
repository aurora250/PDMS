package com.pdm.auth.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission_group")
public class PermissionGroup extends BaseEntity {

    @TableField("group_name")
    private String groupName;

    @TableField("description")
    private String description;

    @TableField("permissions")
    private String permissions; // JSON array of permission strings
}
