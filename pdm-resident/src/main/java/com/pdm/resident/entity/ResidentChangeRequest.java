package com.pdm.resident.entity;

import com.pdm.common.mybatis.BaseNamedEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_change_request")
public class ResidentChangeRequest extends BaseNamedEntity {

    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    @TableField("applicant_uuid")
    private String applicantUuid;

    @TableField("handler_id_list")
    private String handlerIdList;

    @TableField("change_field")
    private String changeField;

    @TableField("request_time")
    private LocalDate requestTime;

    @TableField("original_data")
    private String originalData;

    @TableField("modified_data")
    private String modifiedData;

    @TableField("status")
    private String status;
}
