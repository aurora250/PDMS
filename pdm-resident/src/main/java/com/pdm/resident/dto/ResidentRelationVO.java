package com.pdm.resident.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 居民关系视图对象 — 含UUID对应的姓名。
 */
@Data
public class ResidentRelationVO {

    private String relationPersonUuid;

    private String fatherUuid;
    private String fatherName;

    private String motherUuid;
    private String motherName;

    private String spouseUuid;
    private String spouseName;

    /** 子女列表（逆查：谁以此人为父/母） */
    private List<Map<String, Object>> children;
}
