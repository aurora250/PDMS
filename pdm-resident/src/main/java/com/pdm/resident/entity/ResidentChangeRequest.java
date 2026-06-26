package com.pdm.resident.entity;

import com.pdm.common.mybatis.BaseNamedEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 常住人口信息变更申请实体类
 * 对应数据库表 resident_change_request
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resident_change_request")
public class ResidentChangeRequest extends BaseNamedEntity {

    /**
     * 申请单主键ID
     */
    @TableId(type = IdType.AUTO)
    @TableField("rid")
    private Long rid;

    /**
     * 申请人UUID
     */
    @TableField("applicant_uuid")
    private String applicantUuid;

    /**
     * 处理人ID列表（分号分隔）
     */
    @TableField("handler_id_list")
    private String handlerIdList;

    /**
     * 变更字段名称
     */
    @TableField("change_field")
    private String changeField;

    /**
     * 申请时间
     */
    @TableField("request_time")
    private LocalDate requestTime;

    /**
     * 原始数据（JSON格式）
     */
    @TableField("original_data")
    private String originalData;

    /**
     * 修改后数据（JSON格式）
     */
    @TableField("modified_data")
    private String modifiedData;

    /**
     * 申请状态（请求/通过/驳回）
     */
    @TableField("status")
    private String status;
}