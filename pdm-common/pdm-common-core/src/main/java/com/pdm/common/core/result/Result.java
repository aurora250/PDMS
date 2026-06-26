package com.pdm.common.core.result;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应结果封装。
 *
 * <p>泛型 {@code T} 为响应数据类型。提供一系列静态工厂方法用于快速构建成功/失败响应， 并支持通过 {@link ErrorCode} 枚举直接转换为失败响应。
 *
 * @param <T> 响应数据的类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码，200 表示成功 */
    private int code;

    /** 提示消息 */
    private String message;

    /** 响应数据，为 null 时不序列化到 JSON */
    private T data;

    /** 响应时间戳（毫秒） */
    private Long timestamp;

    /**
     * 构建成功响应（无数据）。
     *
     * @param <T> 数据类型
     * @return 成功响应，code=200
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null, System.currentTimeMillis());
    }

    /**
     * 构建成功响应（带数据）。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应，code=200
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, System.currentTimeMillis());
    }

    /**
     * 构建成功响应（自定义消息 + 数据）。
     *
     * @param message 提示消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应，code=200
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, System.currentTimeMillis());
    }

    /**
     * 构建失败响应（自定义错误码 + 消息）。
     *
     * @param code 错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 失败响应
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    /**
     * 构建失败响应（自定义消息，错误码默认 500）。
     *
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 失败响应
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null, System.currentTimeMillis());
    }

    /**
     * 构建失败响应（使用 {@link ErrorCode} 枚举）。
     *
     * @param errorCode 错误码枚举
     * @param <T> 数据类型
     * @return 失败响应
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(
                errorCode.getCode(), errorCode.getMessage(), null, System.currentTimeMillis());
    }

    /**
     * 判断是否为成功响应。
     *
     * @return true 表示 code == 200
     */
    public boolean isSuccess() {
        return code == 200;
    }
}
