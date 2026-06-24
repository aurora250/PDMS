package com.pdm.auth.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    @TableField("user_uuid")
    private String userUuid;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("token")
    private String token;

    @TableField("resident_uuid")
    private String residentUuid;

    @TableField("user_role")
    private String userRole;

    @TableField("permission_group_id")
    private Long permissionGroupId;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("last_login_ip")
    private String lastLoginIp;

    @TableField("failed_login_count")
    private Integer failedLoginCount;

    @TableField("locked_until")
    private LocalDateTime lockedUntil;

    @TableField("register_materials")
    private String registerMaterials;

    @TableField("account_status")
    private String accountStatus;

    @TableField("must_change_password")
    private Boolean mustChangePassword;
}
