package com.pdm.common.security.annotation;

import java.lang.annotation.*;

/** 标注需要记录审计日志的方法. 操作类型和对象类型由业务自行提供. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** 操作类型: 新增/修改/删除 */
    String operationType();

    /** 操作对象类型, 如 resident, household_register */
    String targetType();

    /** 目标ID的SpEL表达式, 如 #result.data.uuid 或 #uuid */
    String targetIdExpression() default "";
}
