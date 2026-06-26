package com.pdm.common.core.constant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 学历代码常量 - 符合 GB/T 4658-2006 标准
 *
 * <p>
 * 标准名称：学历代码
 * <p>
 * 本类从外部JSON配置文件加载学历标准代码和名称映射
 *
 * @see <a href=
 *      "http://openstd.samr.gov.cn/bzgk/gb/newGbInfo?hcno=86BA879C80B09537B685A7FF87B86906">GB/T
 *      4658-2006</a>
 */
public final class EducationConstants {

    /** 学历代码到名称的映射 */
    private static final Map<String, String> CODE_TO_NAME;

    /** 学历名称到代码的映射 */
    private static final Map<String, String> NAME_TO_CODE;

    static {
        Map<String, String> codeToName = new HashMap<>();
        Map<String, String> nameToCode = new HashMap<>();

        try {
            loadFromConfig(codeToName, nameToCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load GB education codes from config", e);
        }

        CODE_TO_NAME = Collections.unmodifiableMap(codeToName);
        NAME_TO_CODE = Collections.unmodifiableMap(nameToCode);
    }

    private EducationConstants() {
    }

    /**
     * 从外部JSON配置文件加载学历数据
     */
    private static void loadFromConfig(Map<String, String> codeToName, Map<String, String> nameToCode)
            throws Exception {
        try (InputStream is = EducationConstants.class.getResourceAsStream("/gb-education-codes.json")) {
            if (is == null) {
                throw new RuntimeException("GB education codes config file not found: /gb-education-codes.json");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            JsonNode educations = root.get("educations");

            educations.forEach(node -> {
                String code = node.get("code").asText();
                String name = node.get("name").asText();
                codeToName.put(code, name);
                nameToCode.put(name, code);
            });
        }
    }

    /**
     * 验证学历代码是否有效
     *
     * @param code
     *            学历代码（10-99）
     * @return true 如果代码有效
     */
    public static boolean isValidCode(String code) {
        return code != null && CODE_TO_NAME.containsKey(code);
    }

    /**
     * 验证学历名称是否有效
     *
     * @param name
     *            学历名称
     * @return true 如果名称有效
     */
    public static boolean isValidName(String name) {
        return name != null && NAME_TO_CODE.containsKey(name);
    }

    /**
     * 根据代码获取学历名称
     *
     * @param code
     *            学历代码
     * @return 学历名称，如果代码无效返回null
     */
    public static String getNameByCode(String code) {
        return CODE_TO_NAME.get(code);
    }

    /**
     * 根据名称获取学历代码
     *
     * @param name
     *            学历名称
     * @return 学历代码，如果名称无效返回null
     */
    public static String getCodeByName(String name) {
        return NAME_TO_CODE.get(name);
    }

    /**
     * 获取所有学历代码到名称的映射
     *
     * @return 不可变的代码到名称映射
     */
    public static Map<String, String> getAllEducations() {
        return CODE_TO_NAME;
    }
}
