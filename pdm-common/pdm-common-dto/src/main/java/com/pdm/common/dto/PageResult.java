package com.pdm.common.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页响应结果 DTO。
 *
 * <p>
 * 泛型 {@code T} 为列表元素类型。封装分页查询的返回数据， 包含当前页记录、总条数、页码、每页条数和总页数。
 * </p>
 *
 * @param <T>
 *            列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据列表 */
    private List<T> records = Collections.emptyList();
    /** 总记录数 */
    private long total = 0;
    /** 当前页码 */
    private int page = 1;
    /** 每页条数 */
    private int size = 20;
    /** 总页数 */
    private int totalPages = 0;

    /**
     * 构建分页结果。
     *
     * @param records
     *            当前页数据
     * @param total
     *            总记录数
     * @param page
     *            当前页码
     * @param size
     *            每页条数
     * @param <T>
     *            列表元素类型
     * @return 分页结果对象
     */
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(records, total, page, size, totalPages);
    }

    /**
     * 构建空分页结果。
     *
     * @param <T>
     *            列表元素类型
     * @return 空的分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>();
    }
}
