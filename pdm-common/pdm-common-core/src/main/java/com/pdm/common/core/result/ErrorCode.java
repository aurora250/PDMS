package com.pdm.common.core.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // System errors 1000-1999
    SYSTEM_ERROR(1000, "系统内部错误"), SERVICE_UNAVAILABLE(1001, "服务暂不可用"), PARAM_ERROR(1002,
            "参数校验失败"), METHOD_NOT_SUPPORTED(1003,
                    "不支持的请求方法"), DATA_NOT_FOUND(1004, "数据不存在"), DATA_DUPLICATE(1005, "数据重复"),

    // Auth errors 2000-2999
    UNAUTHORIZED(2000, "未登录或登录已过期"), FORBIDDEN(2001, "无访问权限"), TOKEN_EXPIRED(2002, "Token已过期"), TOKEN_INVALID(2003,
            "Token无效"), USERNAME_OR_PASSWORD_ERROR(2004, "用户名或密码错误"), ACCOUNT_LOCKED(2005,
                    "账号已被锁定，请稍后再试"), ACCOUNT_DISABLED(2006, "账号已被禁用"), ACCOUNT_FROZEN(2007,
                            "账号已被冻结"), ACCOUNT_CANCELLED(2008, "账号已被注销"), ACCOUNT_PENDING_APPROVAL(2009,
                                    "账号正在审批中"), PASSWORD_WEAK(2010,
                                            "密码强度不足，需8-16位包含大小写字母+数字+特殊字符"), FIRST_LOGIN_MUST_CHANGE_PASSWORD(2011,
                                                    "首次登录必须修改密码"), LOGIN_FAILED_EXCEED_LIMIT(2012, "连续登录失败超过限制，账号已锁定"), OLD_PASSWORD_ERROR(2013, "原密码错误"),

    // Resident errors 3000-3999
    RESIDENT_NOT_FOUND(3000, "户籍人员不存在"), ID_CARD_INVALID(3001, "身份证号格式不正确"), ID_CARD_DUPLICATE(3002,
            "身份证号已存在"), RELATION_CIRCULAR(3003, "不允许循环亲属关系"), RESIDENT_STATUS_INVALID(3004, "人员状态不允许此操作"),

    // Household errors 4000-4999
    HOUSEHOLD_BOOK_NOT_FOUND(4000, "户口本不存在"), HOUSEHOLD_BUSINESS_DUPLICATE(4001, "该户籍业务申请已存在"), MIGRATION_NOT_FOUND(
            4002, "迁移申请不存在"), APPROVAL_FLOW_ERROR(4003, "审批流程异常"), PERMIT_ALREADY_ISSUED(4004, "证件已签发"),

    // Key person errors 5000-5999
    KEY_PERSON_NOT_FOUND(5000, "重点人员不存在"), KEY_PERSON_ALREADY_EXISTS(5001, "该人员已是重点管控对象"), VISIT_PLAN_NOT_FOUND(5002,
            "走访计划不存在"), VISIT_PLAN_OVERDUE(5003, "走访计划已逾期"),

    // Floating population errors 6000-6999
    FP_RECORD_NOT_FOUND(6000, "流动人口登记记录不存在"), RESIDENT_PERMIT_NOT_FOUND(6001, "居住证不存在"), RESIDENT_PERMIT_EXPIRED(6002,
            "居住证已过期"), RESIDENT_PERMIT_RENEWAL_EXISTS(6003, "已有续期记录"),

    // Missing person errors 7000-7999
    MISSING_PERSON_NOT_FOUND(7000, "失踪人员记录不存在"), MISSING_PERSON_ALREADY_RECOVERED(7001, "失踪人员已找回"),

    // Import/Export errors 8000-8999
    IMPORT_TEMPLATE_ERROR(8000, "导入模板格式错误"), IMPORT_DATA_ERROR(8001, "导入数据校验失败"), EXPORT_ERROR(8002, "导出失败");

    private final int code;
    private final String message;
}
