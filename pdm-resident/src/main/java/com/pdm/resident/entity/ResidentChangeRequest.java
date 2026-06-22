package com.pdm.resident.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_change_request")
public class ResidentChangeRequest extends BaseEntity {

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
