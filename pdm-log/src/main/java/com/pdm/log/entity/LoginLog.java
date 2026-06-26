package com.pdm.log.entity;

import com.pdm.common.mybatis.BaseNamedEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登录日志实体类
 * 对应数据库表login_log，存储用户登录相关信息
 *
 * @author 开发者
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("login_log")
public class LoginLog extends BaseNamedEntity {

    /**
     * 日志主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    @TableField("log_id")
    private Long logId;

    /**
     * 登录用户唯一标识
     */
    @TableField("user_uuid")
    private String userUuid;

    /**
     * 登录时间
     */
    @TableField("login_time")
    private LocalDateTime loginTime;

    /**
     * 登录IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 登录是否成功（1：成功，0：失败）
     */
    @TableField("is_success")
    private Integer isSuccess;

    /**
     * 登录失败原因（如：密码错误、账号不存在）
     */
    @TableField("fail_reason")
    private String failReason;
}