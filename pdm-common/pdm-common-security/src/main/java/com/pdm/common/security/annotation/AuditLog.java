package com.pdm.common.security.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解。
 *
 * <p>
 * 标注在需要记录操作审计日志的方法上（通常为 Controller 或 Service 层方法）。 配合 AOP
 * 切面实现自动记录操作类型、目标对象类型及目标 ID。
 * </p>
 *
 * <p>
 * 使用示例：
 *
 * <pre>{@code
 * &#64;AuditLog(operationType = "删除", targetType = "user", targetIdExpression = "#uuid")
 * public Result<Void> deleteUser(@PathVariable String uuid) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** 操作类型，如"新增""修改""删除" */
    String operationType();

    /** 操作对象类型，如 {@code resident}、{@code household_register} */
    String targetType();

    /** 目标 ID 的 SpEL 表达式，如 {@code #result.data.uuid} 或 {@code #uuid} */
    String targetIdExpression() default "";
}
