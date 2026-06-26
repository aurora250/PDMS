package com.pdm.resident.dto;

import com.pdm.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 常住人口查询请求DTO
 * 继承分页请求基础属性，扩展常住人口多条件查询字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResidentSearchRequest extends PageRequest {

    /**
     * 姓名
     */
    private String name;

    /**
     * 性别
     */
    private String gender;

    /**
     * 民族
     */
    private String nation;

    /**
     * 文化程度
     */
    private String educationLevel;

    /**
     * 婚姻状况
     */
    private String maritalStatus;

    /**
     * 户口状态
     */
    private String householdStatus;

    /**
     * 身份证号
     */
    private String idCardNo;

    /**
     * 最小年龄
     */
    private Integer minAge;

    /**
     * 最大年龄
     */
    private Integer maxAge;
}