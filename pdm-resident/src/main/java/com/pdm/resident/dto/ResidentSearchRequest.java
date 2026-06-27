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
    private String idCardNo;
    private Integer minAge;
    private Integer maxAge;
}
