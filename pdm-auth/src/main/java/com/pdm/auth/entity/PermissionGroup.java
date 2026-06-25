package com.pdm.auth.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission_group")
public class PermissionGroup extends BaseNamedEntity {

    @TableId(type = IdType.AUTO)
    @TableField("group_id")
    private Long groupId;

    @TableField("group_name")
    private String groupName;

    @TableField("description")
    private String description;

    @TableField("permissions")
    private String permissions; // JSON array of permission strings
}
