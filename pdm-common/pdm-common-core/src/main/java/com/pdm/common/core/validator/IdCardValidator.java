package com.pdm.common.core.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 身份证号校验器 - 完全符合 GB 11643-1999 标准
 *
 * <p>标准要求：
 * <ul>
 *   <li>1. 18位数字（最后一位可为X）</li>
 *   <li>2. 前6位为有效地区码（必须在GB/T 2260中存在）</li>
 *   <li>3. 第7-14位为出生日期（yyyyMMdd）</li>
 *   <li>4. 第15-17位为顺序码（第17位表示性别）</li>
 *   <li>5. 第18位为校验码（按ISO 7064:1983.MOD 11-2算法计算）</li>
 * </ul>
 *
 * @see <a href="http://www.stats.gov.cn/">国家标准 GB 11643-1999</a>
 */
public final class IdCardValidator {

    private static final int[] WEIGHT = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2 };
    private static final char[] CHECK_CODE = { '1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2' };
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 有效地区码集合 - 从外部配置文件加载（GB/T 2260-2007） */
    private static final Set<String> VALID_AREA_CODES = loadValidAreaCodes();

    private IdCardValidator() {
    }

    /**
     * 从外部JSON配置文件加载有效地区码
     *
     * @return 不可变的地区码集合
     * @throws RuntimeException 如果配置文件加载失败
     */
    private static Set<String> loadValidAreaCodes() {
        try (InputStream is = IdCardValidator.class.getResourceAsStream("/gb-area-codes.json")) {
            if (is == null) {
                throw new RuntimeException("GB area codes config file not found: /gb-area-codes.json");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            Set<String> codes = new HashSet<>();
            root.get("codes").forEach(node -> codes.add(node.asText()));
            return Collections.unmodifiableSet(codes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load GB area codes from config", e);
        }
    }

    /**
     * 验证身份证号是否有效
     *
     * @param idCardNo 18位身份证号
     * @return true 如果身份证号完全符合GB 11643-1999标准
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

        // 地区码校验（前6位必须在GB/T 2260-2007中存在）
        String areaCode = idCardNo.substring(0, 6);
        if (!isValidAreaCode(areaCode)) {
            return false;
        }

        // 出生日期合法性校验（第7-14位）
        try {
            LocalDate birthDate = LocalDate.parse(idCardNo.substring(6, 14), DATE_FORMATTER);
            // 出生日期不能晚于当前日期
            if (birthDate.isAfter(LocalDate.now())) {
                return false;
            }
            // 出生日期不能早于1900年（合理性校验）
            if (birthDate.isBefore(LocalDate.of(1900, 1, 1))) {
                return false;
            }
        } catch (DateTimeParseException e) {
            return false;
        }

        // 校验码验证（第18位）
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
     * 验证地区码是否有效（符合GB/T 2260-2007标准）
     *
     * @param areaCode 6位地区码
     * @return true 如果地区码存在于标准中
     */
    public static boolean isValidAreaCode(String areaCode) {
        return areaCode != null && VALID_AREA_CODES.contains(areaCode);
    }

    /**
     * 提取身份证号中的地区码（前6位）
     *
     * @param idCardNo 18位身份证号
     * @return 6位地区码
     * @throws IllegalArgumentException 如果身份证号无效
     */
    public static String extractAreaCode(String idCardNo) {
        if (!isValid(idCardNo)) {
            throw new IllegalArgumentException("Invalid ID card number");
        }
        return idCardNo.substring(0, 6);
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
