package com.pdm.resident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentImportResult {
    private int totalCount;
    private int successCount;
    private int failCount;
    private List<String> errorMessages;
}
