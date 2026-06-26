package com.pdm.common.core.exception;

import com.pdm.common.core.result.ErrorCode;

import lombok.Getter;

/**
 * 业务异常类。
 *
 * <p>
 * 用于在各层抛出可预见的业务逻辑异常，由全局异常处理器统一捕获并转换为前端友好的错误响应。 支持直接传入 {@link ErrorCode}
 * 枚举、自定义错误码 + 消息、以及附加详细信息。
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    /**
     * 使用自定义错误码和消息构造异常。
     *
     * @param code
     *            错误码
     * @param message
     *            错误描述
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用自定义消息构造异常，错误码默认为 500。
     *
     * @param message
     *            错误描述
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 使用 {@link ErrorCode} 枚举构造异常。
     *
     * @param errorCode
     *            错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用 {@link ErrorCode} 枚举和附加详细信息构造异常。
     *
     * @param errorCode
     *            错误码枚举
     * @param detail
     *            附加详细信息，将拼接在错误消息后
     */
    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.code = errorCode.getCode();
    }
}
