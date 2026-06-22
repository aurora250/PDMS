package com.pdm.keyperson.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("key_person")
public class KeyPerson extends BaseEntity {

    @TableField("uuid")
    private String uuid;

    @TableField("control_level")
    private String controlLevel;

    @TableField("control_type")
    private String controlType;

    @TableField("designated_at")
    private LocalDateTime designatedAt;

    @TableField("revoked_at")
    private LocalDateTime revokedAt;

    @TableField("responsible_police_no")
    private String responsiblePoliceNo;
}
