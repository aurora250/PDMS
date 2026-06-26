package com.pdm.resident.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 常住人口Excel导入结果DTO
 * 用于返回导入的总数、成功数、失败数及错误信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentImportResult {
    /**
     * 总导入数量
     */
    private int totalCount;

    /**
     * 导入成功数量
     */
    private int successCount;

    /**
     * 导入失败数量
     */
    private int failCount;

    /**
     * 失败原因列表
     */
    private List<String> errorMessages;
}