package com.pdm.common.core.validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 身份证号校验器。
 *
 * <p>
 * 符合 GB 11643-1999 标准，支持以下功能：
 * <ul>
 * <li>校验 18 位身份证号格式及校验码正确性</li>
 * <li>从身份证号中提取出生日期</li>
 * <li>从身份证号中提取性别（第 17 位，奇数为男性，偶数为女性）</li>
 * </ul>
 *
 * <p>
 * 工具类不可实例化。
 * </p>
 */
public final class IdCardValidator {

    /** 加权因子，用于计算校验码 */
    private static final int[] WEIGHT = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2 };
    /** 校验码映射表，索引为 sum % 11 的结果 */
    private static final char[] CHECK_CODE = { '1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2' };
    /** 身份证中出生日期部分的格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private IdCardValidator() {
    }

    /**
     * 校验身份证号格式及校验码是否正确。
     *
     * <p>
     * 校验规则：18 位长度，前 17 位为数字，第 18 位为校验码（0-9 或 X/x）。
     * </p>
     *
     * @param idCardNo
     *            18 位身份证号
     * @return true 表示校验通过
     */
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

    /**
     * 从身份证号中提取出生日期（第 7-14 位）。
     *
     * @param idCardNo
     *            18 位身份证号
     * @return 出生日期
     * @throws IllegalArgumentException
     *             身份证号无效时抛出
     */
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
