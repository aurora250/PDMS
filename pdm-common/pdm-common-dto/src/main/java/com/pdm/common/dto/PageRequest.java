package com.pdm.common.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private int page = 1;
    private int size = 20;
    private String sortBy;
    private String sortDirection = "DESC";

    public int getOffset() {
        return (page - 1) * size;
    }
}
