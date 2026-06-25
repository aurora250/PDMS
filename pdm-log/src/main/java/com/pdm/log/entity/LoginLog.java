package com.pdm.log.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("login_log")
public class LoginLog extends BaseNamedEntity {

    @TableId(type = IdType.AUTO)
    @TableField("log_id")
    private Long logId;

    @TableField("user_uuid")
    private String userUuid;

    @TableField("login_time")
    private LocalDateTime loginTime;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("is_success")
    private Integer isSuccess;

    @TableField("fail_reason")
    private String failReason;
}
