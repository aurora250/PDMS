package com.pdm.missingperson.entity;

import com.pdm.common.mybatis.BaseNamedEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 失踪人员寻回记录实体类
 * 对应数据库表missing_person_recovery，记录失踪人员寻回的相关信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("missing_person_recovery")
public class MissingPersonRecovery extends BaseNamedEntity {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    /**
     * 关联的失踪人员记录主键ID
     */
    @TableField("missing_record_rid")
    private Long missingRecordRid;

    /**
     * 寻回日期
     */
    @TableField("recovery_date")
    private LocalDate recoveryDate;

    /**
     * 寻回情况概述
     */
    @TableField("summary")
    private String summary;
}