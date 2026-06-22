package com.pdm.keyperson.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("petition_record")
public class PetitionRecord extends BaseEntity {

    @TableField("key_person_uuid")
    private String keyPersonUuid;

    @TableField("handler_police_no")
    private String handlerPoliceNo;

    @TableField("petition_time")
    private LocalDateTime petitionTime;

    @TableField("address")
    private String address;

    @TableField("remark")
    private String remark;

    @TableField("evaluation")
    private String evaluation;
}
