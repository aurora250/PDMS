package com.pdm.common.core.validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 身份证号校验器 — 符合 GB 11643-1999 标准.
 *
 * <p>校验规则: 18位长度, 前17位数字, 最后一位校验码, 自动提取出生日期和性别.
 */
public final class IdCardValidator {

    private static final int[] WEIGHT =
            {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODE =
            {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private IdCardValidator() {}

    public static boolean isValid(String idCardNo) {
        if (idCardNo == null || idCardNo.length() != 18) {
            return false;
        }
        // 前17位必须为数字
        for (int i = 0; i < 17; i++) {
            if (!Character.isDigit(idCardNo.charAt(i))) {
                return false;
            }
        }
        // 校验码
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCardNo.charAt(i) - '0') * WEIGHT[i];
        }
        char expectedCheck = CHECK_CODE[sum % 11];
        char actualCheck = idCardNo.charAt(17);
        if (expectedCheck == 'X') {
            return actualCheck == 'X' || actualCheck == 'x';
        }
        return actualCheck == expectedCheck;
    }

    /** Extract birth date from ID card number (positions 7-14). */
    public static LocalDate extractBirthDate(String idCardNo) {
        if (!isValid(idCardNo)) {
            throw new IllegalArgumentException("Invalid ID card number");
        }
        return LocalDate.parse(idCardNo.substring(6, 14), DATE_FORMATTER);
    }

    /**
     * Extract gender from ID card number.
     *
     * @return "男" if the 17th digit is odd, "女" if even
     */
    public static String extractGender(String idCardNo) {
        if (!isValid(idCardNo)) {
            throw new IllegalArgumentException("Invalid ID card number");
        }
        int genderDigit = idCardNo.charAt(16) - '0';
        return genderDigit % 2 == 1 ? "男" : "女";
    }
}
