package com.pdm.auth.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户实体类，对应数据库 {@code sys_user} 表。
 *
 * <p>
 * 继承 {@link BaseEntity}，自动获得 ID、创建时间、更新时间、逻辑删除等通用字段。
 * 记录用户的登录凭证、角色权限、账号安全状态（锁定/失败计数）以及关联的实名信息。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    /** 用户唯一标识 UUID */
    @TableField("user_uuid")
    private String userUuid;

    /** 登录用户名 */
    @TableField("username")
    private String username;

    /** BCrypt 加密后的密码 */
    @TableField("password")
    private String password;

    /** 当前有效的 JWT 访问令牌 */
    @TableField("token")
    private String token;

    /** 关联的居民实名认证 UUID */
    @TableField("resident_uuid")
    private String residentUuid;

    /** 用户角色（系统管理员 / 用户管理员 / 普通用户） */
    @TableField("user_role")
    private String userRole;

    /** 关联的权限组 ID */
    @TableField("permission_group_id")
    private Long permissionGroupId;

    /** 手机号 */
    @TableField("phone")
    private String phone;

    /** 电子邮箱 */
    @TableField("email")
    private String email;

    /** 最近一次登录时间 */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /** 最近一次登录 IP 地址 */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /** 连续登录失败次数，成功后归零 */
    @TableField("failed_login_count")
    private Integer failedLoginCount;

    /** 账号锁定截止时间，null 表示未锁定 */
    @TableField("locked_until")
    private LocalDateTime lockedUntil;

    /** 注册时提交的审核材料（JSON 或文件路径） */
    @TableField("register_materials")
    private String registerMaterials;

    /** 账号状态（审批中 / 有效 / 冻结 / 注销 / 锁定） */
    @TableField("account_status")
    private String accountStatus;

    /** 是否强制在下次登录时修改密码 */
    @TableField("must_change_password")
    private Boolean mustChangePassword;
}
