package com.pdm.common.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页请求 DTO。
 *
 * <p>
 * 封装前端传入的分页查询参数，包含页码、每页条数、排序字段和排序方向。 默认值：page=1, size=20, sortDirection=DESC。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码，从 1 开始 */
    private int page = 1;
    /** 每页条数，默认 20 */
    private int size = 20;
    /** 排序字段名 */
    private String sortBy;
    /** 排序方向，默认 DESC */
    private String sortDirection = "DESC";

    /**
     * 计算分页偏移量。
     *
     * @return SQL 查询的 offset 值
     */
    public int getOffset() {
        return (page - 1) * size;
    }
}
