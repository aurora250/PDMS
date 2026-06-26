package com.pdm.keyperson.entity;

import com.pdm.common.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 信访记录实体类
 * 对应数据库表：petition_record，存储重点人员信访相关信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("petition_record")
public class PetitionRecord extends BaseEntity {
    /**
     * 重点人员唯一标识
     */
    @TableField("key_person_uuid")
    private String keyPersonUuid;
    /**
     * 处理民警编号
     */
    @TableField("handler_police_no")
    private String handlerPoliceNo;
    /**
     * 信访时间
     */
    @TableField("petition_time")
    private LocalDateTime petitionTime;

    /**
     * 信访地址
     */
    @TableField("address")
    private String address;
    /**
     * 信访备注
     */
    @TableField("remark")
    private String remark;
    /**
     * 信访评价
     */
    @TableField("evaluation")
    private String evaluation;
}
