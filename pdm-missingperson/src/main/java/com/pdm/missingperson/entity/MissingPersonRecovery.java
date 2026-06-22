package com.pdm.missingperson.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("missing_person_recovery")
public class MissingPersonRecovery extends BaseEntity {

    @TableField("missing_record_rid")
    private Long missingRecordRid;

    @TableField("recovery_date")
    private LocalDate recoveryDate;

    @TableField("summary")
    private String summary;
}
