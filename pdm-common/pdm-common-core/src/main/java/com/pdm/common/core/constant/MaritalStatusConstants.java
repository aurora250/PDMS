package com.pdm.common.core.constant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 婚姻状况代码常量 - 符合 GB/T 2261.2-2003 标准
 *
 * <p>
 * 标准名称：个人基本信息分类与代码 第2部分：婚姻状况代码
 * <p>
 * 本类从外部JSON配置文件加载婚姻状况标准代码和名称映射
 *
 * @see <a href=
 *      "http://openstd.samr.gov.cn/bzgk/gb/newGbInfo?hcno=DA7F3FFA75D4B5BD2F1F9BCD0377013D">GB/T
 *      2261.2-2003</a>
 */
public final class MaritalStatusConstants {

    /** 婚姻状况代码到名称的映射 */
    private static final Map<String, String> CODE_TO_NAME;

    /** 婚姻状况名称到代码的映射 */
    private static final Map<String, String> NAME_TO_CODE;

    static {
        Map<String, String> codeToName = new HashMap<>();
        Map<String, String> nameToCode = new HashMap<>();

        try {
            loadFromConfig(codeToName, nameToCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load GB marital status codes from config", e);
        }

        CODE_TO_NAME = Collections.unmodifiableMap(codeToName);
        NAME_TO_CODE = Collections.unmodifiableMap(nameToCode);
    }

    private MaritalStatusConstants() {
    }

    /**
     * 从外部JSON配置文件加载婚姻状况数据
     */
    private static void loadFromConfig(Map<String, String> codeToName, Map<String, String> nameToCode)
            throws Exception {
        try (InputStream is = MaritalStatusConstants.class.getResourceAsStream("/gb-marital-status-codes.json")) {
            if (is == null) {
                throw new RuntimeException(
                        "GB marital status codes config file not found: /gb-marital-status-codes.json");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            JsonNode statuses = root.get("statuses");

            statuses.forEach(node -> {
                String code = node.get("code").asText();
                String name = node.get("name").asText();
                codeToName.put(code, name);
                nameToCode.put(name, code);
            });
        }
    }

    /**
     * 验证婚姻状况代码是否有效
     *
     * @param code
     *            婚姻状况代码（10/20/21/22/23/30/40/90）
     * @return true 如果代码有效
     */
    public static boolean isValidCode(String code) {
        return code != null && CODE_TO_NAME.containsKey(code);
    }

    /**
     * 验证婚姻状况名称是否有效
     *
     * @param name
     *            婚姻状况名称
     * @return true 如果名称有效
     */
    public static boolean isValidName(String name) {
        return name != null && NAME_TO_CODE.containsKey(name);
    }

    /**
     * 根据代码获取婚姻状况名称
     *
     * @param code
     *            婚姻状况代码
     * @return 婚姻状况名称，如果代码无效返回null
     */
    public static String getNameByCode(String code) {
        return CODE_TO_NAME.get(code);
    }

    /**
     * 根据名称获取婚姻状况代码
     *
     * @param name
     *            婚姻状况名称
     * @return 婚姻状况代码，如果名称无效返回null
     */
    public static String getCodeByName(String name) {
        return NAME_TO_CODE.get(name);
    }

    /**
     * 获取所有婚姻状况代码到名称的映射
     *
     * @return 不可变的代码到名称映射
     */
    public static Map<String, String> getAllStatuses() {
        return CODE_TO_NAME;
    }
}
