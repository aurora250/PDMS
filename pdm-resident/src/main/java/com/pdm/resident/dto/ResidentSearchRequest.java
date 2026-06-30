package com.pdm.resident.dto;

import com.pdm.common.dto.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResidentSearchRequest extends PageRequest {

    private String name;
    private String gender;
    private String nation;
    private String nationCode; // 民族代码 - GB 3304-1991
    private String educationLevel;
    private String educationCode; // 学历代码 - GB/T 4658-2006
    private String maritalStatus;
    private String householdStatus;
    private String province;
    private String idCardNo;
    private Integer minAge;
    private Integer maxAge;

    /**
     * 生成缓存键，包含所有搜索条件和分页参数.
     */
    public String cacheKey() {
        return String.format("rs:%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%d|%d|%d", name, gender, nation, nationCode, educationLevel,
                educationCode, maritalStatus, householdStatus, province, idCardNo, minAge, maxAge,
                getPage() * 1000 + getSize());
    }
}
