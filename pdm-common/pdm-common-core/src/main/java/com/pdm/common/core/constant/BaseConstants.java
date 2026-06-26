package com.pdm.common.core.constant;

/**
 * 系统全局常量定义类。
 *
 * <p>集中管理 JWT 配置、安全策略、业务周期、Redis Key 前缀及逻辑删除标记等常量。 工具类不可实例化。
 */
public final class BaseConstants {

    private BaseConstants() {}

    /** JWT Token 有效期: 2小时 (毫秒) */
    public static final long JWT_EXPIRATION_MS = 2 * 60 * 60 * 1000L;

    /** JWT Token 刷新阈值: 剩余有效期小于30分钟时刷新 */
    public static final long JWT_REFRESH_THRESHOLD_MS = 30 * 60 * 1000L;

    /** 登录失败锁定: 最大失败次数 */
    public static final int MAX_LOGIN_FAIL_COUNT = 5;

    /** 登录失败锁定: 锁定时长 (分钟) */
    public static final int LOGIN_LOCK_DURATION_MINUTES = 30;

    /** 密码最小长度 */
    public static final int PASSWORD_MIN_LENGTH = 8;

    /** 密码最大长度 */
    public static final int PASSWORD_MAX_LENGTH = 16;

    /** 居住证到期提醒: 提前天数 */
    public static final int PERMIT_EXPIRY_WARN_DAYS = 7;

    /** 重点人员走访周期: 一级 (天) */
    public static final int CONTROL_LEVEL_1_INTERVAL = 7;

    /** 重点人员走访周期: 二级 (天) */
    public static final int CONTROL_LEVEL_2_INTERVAL = 30;

    /** 重点人员走访周期: 三级 (天) */
    public static final int CONTROL_LEVEL_3_INTERVAL = 90;

    /** Redis Key 前缀 */
    public static final String REDIS_KEY_PREFIX = "pdm:";

    /** Token 黑名单 Key 前缀 */
    public static final String TOKEN_BLACKLIST_PREFIX = REDIS_KEY_PREFIX + "token:blacklist:";

    /** 逻辑删除: 未删除 */
    public static final int NOT_DELETED = 0;

    /** 逻辑删除: 已删除 */
    public static final int DELETED = 1;
}
