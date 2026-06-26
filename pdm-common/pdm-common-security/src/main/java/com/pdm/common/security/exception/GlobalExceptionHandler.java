package com.pdm.common.security.exception;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.common.core.result.Result;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器。
 *
 * <p>使用 {@code @RestControllerAdvice} 统一拦截所有 Controller 层抛出的异常， 将其转换为 {@link Result} 格式的标准错误响应。
 * 覆盖以下几类异常：
 *
 * <ul>
 *   <li>{@code BusinessException} —— 业务异常，返回对应的错误码和消息
 *   <li>参数校验异常（{@code MethodArgumentNotValidException} / {@code BindException} / {@code
 *       ConstraintViolationException}）—— 400 + 具体校验信息
 *   <li>{@code AccessDeniedException} —— 403 无权限
 *   <li>{@code AuthenticationException} / {@code BadCredentialsException} —— 401 未授权
 *   <li>{@code Exception} —— 兜底处理，500 系统内部错误
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * <p>记录 warn 级别的日志后，返回异常中携带的错误码和消息。
     *
     * @param e 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理请求体 / 表单参数校验异常（400）。
     *
     * <p>提取所有字段校验错误信息，以分号分隔后返回。
     *
     * @param e 校验异常
     * @return 失败响应（含字段级错误详情）
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(Exception e) {
        String message = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex) {
            message =
                    ex.getBindingResult().getFieldErrors().stream()
                            .map(f -> f.getField() + ": " + f.getDefaultMessage())
                            .reduce((a, b) -> a + "; " + b)
                            .orElse(message);
        }
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理方法级别约束校验异常（400）。
     *
     * @param e 约束违反异常
     * @return 失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理 Spring Security 权限拒绝异常（403）。
     *
     * @param e 权限拒绝异常
     * @return 失败响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        return Result.fail(ErrorCode.FORBIDDEN);
    }

    /**
     * 处理 Spring Security 认证异常（401），如未登录或令牌过期。
     *
     * @param e 认证异常
     * @return 失败响应
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        return Result.fail(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 处理密码错误异常（401）。
     *
     * @param e 密码错误异常
     * @return 失败响应
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleBadCredentialsException(BadCredentialsException e) {
        return Result.fail(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
    }

    /**
     * 兜底处理未预期的系统异常（500）。
     *
     * <p>记录 error 级别日志并附带堆栈信息，便于问题排查。
     *
     * @param e 未预期的异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
