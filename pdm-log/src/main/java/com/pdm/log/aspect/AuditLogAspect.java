package com.pdm.log.aspect;

import com.pdm.common.security.UserContextHolder;
import com.pdm.common.security.annotation.AuditLog;
import com.pdm.log.service.LogService;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AOP 切面：拦截标注 {@link AuditLog} 的方法，自动记录审计日志。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final LogService logService;

    @Around("@annotation(auditLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLogAnnotation) throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();
            recordLog(joinPoint, auditLogAnnotation, result, null);
        } catch (Throwable t) {
            recordLog(joinPoint, auditLogAnnotation, null, t.getMessage());
            throw t;
        }
        return result;
    }

    private void recordLog(ProceedingJoinPoint joinPoint, AuditLog auditLogAnnotation, Object result, String errorMsg) {
        try {
            com.pdm.log.entity.AuditLog entity = new com.pdm.log.entity.AuditLog();
            entity.setOperationType(auditLogAnnotation.operationType());
            entity.setTargetType(auditLogAnnotation.targetType());
            entity.setOperationTime(LocalDateTime.now());

            // 操作IP
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                entity.setIpAddress(getClientIp(request));
            }

            // 操作人UUID（从 ThreadLocal UserContextHolder 获取）
            String operatorUuid = UserContextHolder.getUserUuid();
            entity.setOperatorUuid(operatorUuid != null ? operatorUuid : "unknown");

            // 解析目标ID（支持SpEL表达式）
            String targetExpr = auditLogAnnotation.targetIdExpression();
            if (targetExpr != null && !targetExpr.isEmpty()) {
                entity.setTargetId(resolveSpel(joinPoint, targetExpr, result));
            }

            // 结果/错误数据
            if (errorMsg != null) {
                entity.setAfterData("{\"error\":\"" + errorMsg + "\"}");
            } else if (result != null) {
                String resultStr = result.toString();
                if (resultStr.length() > 2000) {
                    resultStr = resultStr.substring(0, 2000);
                }
                entity.setAfterData(resultStr);
            }

            logService.recordAuditLog(entity);
        } catch (Exception e) {
            log.warn("Failed to record audit log: {}", e.getMessage());
        }
    }

    private String resolveSpel(ProceedingJoinPoint joinPoint, String expression, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            StandardEvaluationContext context = new StandardEvaluationContext();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            context.setVariable("result", result);

            ExpressionParser parser = new SpelExpressionParser();
            Object value = parser.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.warn("Failed to resolve SpEL expression '{}': {}", expression, e.getMessage());
            return "";
        }
    }

    private static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
