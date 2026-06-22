package com.pdm.common.security;

/**
 * 当前登录用户上下文 — 基于 ThreadLocal, 请求结束后须清理.
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {}

    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static String getUserUuid() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.userUuid() : null;
    }

    public static String getUsername() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.username() : null;
    }

    public static String getRole() {
        UserContext ctx = CONTEXT.get();
        return ctx != null ? ctx.role() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public record UserContext(String userUuid, String username, String role, String ipAddress) {}
}
