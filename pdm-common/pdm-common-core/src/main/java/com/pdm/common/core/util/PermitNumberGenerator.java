package com.pdm.common.core.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 证件编号生成器 — 符合国家标准格式
 *
 * <ul>
 * <li>居住证: 6位区划 + 4位年 + 2位月 + 6位流水 = 18位</li>
 * <li>准迁证: 6位区划 + 4位年 + 6位流水 = 16位</li>
 * <li>迁移证: 6位区划 + 4位年 + 6位流水 = 16位</li>
 * <li>户口簿: 6位区划 + 4位年 + 8位流水 = 18位</li>
 * </ul>
 *
 * 依据:《居住证暂行条例》(2016)、公安部《户口迁移证件管理规定》、GB/T 2260
 */
public final class PermitNumberGenerator {

    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    private PermitNumberGenerator() {
    }

    /** 居住证号: 区划(6) + 年月(6) + 流水(6) = 18位 */
    public static String residentPermit(String areaCode, LocalDate date, long seq) {
        String ac = padAreaCode(areaCode);
        String ym = date != null ? date.format(MONTH_FMT) : LocalDate.now().format(MONTH_FMT);
        return ac + ym + String.format("%06d", seq % 1_000_000);
    }

    /** 准迁证号: 区划(6) + 年(4) + 流水(6) = 16位 */
    public static String approvalPermit(String areaCode, LocalDate date, long seq) {
        String ac = padAreaCode(areaCode);
        String y = date != null ? date.format(YEAR_FMT) : LocalDate.now().format(YEAR_FMT);
        return ac + y + String.format("%06d", seq % 1_000_000);
    }

    /** 迁移证号: 区划(6) + 年(4) + 流水(6) = 16位 */
    public static String migrationPermit(String areaCode, LocalDate date, long seq) {
        String ac = padAreaCode(areaCode);
        String y = date != null ? date.format(YEAR_FMT) : LocalDate.now().format(YEAR_FMT);
        return ac + y + String.format("%06d", seq % 1_000_000);
    }

    /** 户口簿号: 区划(6) + 年(4) + 流水(8) = 18位 */
    public static String householdBook(String areaCode, LocalDate date, long seq) {
        String ac = padAreaCode(areaCode);
        String y = date != null ? date.format(YEAR_FMT) : LocalDate.now().format(YEAR_FMT);
        return ac + y + String.format("%08d", seq % 100_000_000);
    }

    /** 确保行政区划代码为6位数字字符串 */
    private static String padAreaCode(String areaCode) {
        if (areaCode == null || areaCode.isEmpty())
            return "000000";
        return String.format("%6s", areaCode.trim()).replace(' ', '0');
    }
}
