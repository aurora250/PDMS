package com.pdm.common.core.constant;

/**
 * 动态权限字符串常量 — 与 permission_group.permissions JSON 对齐.
 * <p>
 * 命名规范: {@code 模块:资源:操作}
 * <p>
 * 系统管理员组 permissions=["*"] 匹配所有权限.
 */
public final class PermissionConstants {

    /** 超级管理员通配符 — 拥有所有权限 */
    public static final String SUPER_ADMIN = "*";

    // ──────────── Auth 模块 ────────────
    public static final String AUTH_USER_READ = "auth:user:read";
    public static final String AUTH_USER_WRITE = "auth:user:write";
    public static final String AUTH_USER_STATUS = "auth:user:status";
    public static final String AUTH_POLICE_READ = "auth:police:read";
    public static final String AUTH_POLICE_WRITE = "auth:police:write";
    public static final String AUTH_PERMISSION_WRITE = "auth:permission:write";

    // ──────────── Resident 模块 ────────────
    public static final String RESIDENT_READ = "resident:read";
    public static final String RESIDENT_WRITE = "resident:write";
    public static final String RESIDENT_DELETE = "resident:delete";
    public static final String RESIDENT_IMPORT = "resident:import";
    public static final String RESIDENT_EXPORT = "resident:export";
    public static final String RESIDENT_CHANGE_APPROVE = "resident:change-request:approve";
    public static final String RESIDENT_CHANGE_SECOND_APPROVE = "resident:change-request:second-approve";

    // ──────────── Household 模块 ────────────
    public static final String HOUSEHOLD_READ = "household:read";
    public static final String HOUSEHOLD_WRITE = "household:write";
    public static final String HOUSEHOLD_APPROVE = "household:approve";
    public static final String HOUSEHOLD_SECOND_APPROVE = "household:second-approve";
    public static final String HOUSEHOLD_MATERIAL_ATTACH = "household:material:attach";

    // ──────────── KeyPerson 模块 ────────────
    public static final String KEYPERSON_READ = "keyperson:read";
    public static final String KEYPERSON_WRITE = "keyperson:write";
    public static final String KEYPERSON_DELETE = "keyperson:delete";
    public static final String KEYPERSON_VISIT_WRITE = "keyperson:visit-plan:write";
    public static final String KEYPERSON_PETITION_WRITE = "keyperson:petition:write";
    public static final String KEYPERSON_GIS_READ = "keyperson:gis:read";
    public static final String KEYPERSON_REVIEW = "keyperson:review";

    // ──────────── Floating Population 模块 ────────────
    public static final String FP_READ = "fp:read";
    public static final String FP_WRITE = "fp:write";
    public static final String FP_DELETE = "fp:delete";
    public static final String FP_RESIDENCE_WRITE = "fp:residence:write";
    public static final String FP_PERMIT_APPROVE = "fp:permit:approve";
    public static final String FP_PERMIT_ISSUE = "fp:permit:issue";
    public static final String FP_REVIEW = "fp:review";

    // ──────────── Missing Person 模块 ────────────
    public static final String MISSING_READ = "missing:read";
    public static final String MISSING_WRITE = "missing:write";
    public static final String MISSING_DELETE = "missing:delete";
    public static final String MISSING_RECOVERY_WRITE = "missing:recovery:write";
    public static final String MISSING_REVIEW = "missing:review";

    // ──────────── Log 模块 ────────────
    public static final String LOG_AUDIT_READ = "log:audit:read";
    public static final String LOG_LOGIN_READ = "log:login:read";
    public static final String LOG_EXPORT = "log:export";

    // ──────────── Alert 模块 ────────────
    public static final String ALERT_READ = "alert:read";
    public static final String ALERT_HANDLE = "alert:handle";

    // ──────────── 通用 ────────────
    public static final String STATISTICS_READ = "statistics:read";

    private PermissionConstants() {
    }
}
