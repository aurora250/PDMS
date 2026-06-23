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
    private String educationLevel;
    private String maritalStatus;
    private String householdStatus;
    private String idCardNo;
    private Integer minAge;
    private Integer maxAge;
}
