package com.pdm.keyperson.entity;

import com.pdm.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 重点人员实体类
 * 对应数据库表：key_person，存储重点人员基础信息和管控状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("key_person")
public class KeyPerson extends BaseEntity {

    /**
     * 重点人员唯一标识
     */
    @TableField("uuid")
    private String uuid;

    /**
     * 管控等级（一级/二级/其他）
     */
    @TableField("control_level")
    private String controlLevel;

    /**
     * 管控类型（如：治安管控/重点稳控等）
     */
    @TableField("control_type")
    private String controlType;

    /**
     * 认定为重点人员的时间
     */
    @TableField("designated_at")
    private LocalDateTime designatedAt;

    /**
     * 撤销重点人员身份的时间
     */
    @TableField("revoked_at")
    private LocalDateTime revokedAt;

    /**
     * 责任民警编号
     */
    @TableField("responsible_police_no")
    private String responsiblePoliceNo;
}