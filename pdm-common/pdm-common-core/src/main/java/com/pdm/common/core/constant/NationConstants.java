package com.pdm.common.core.constant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 民族代码常量 - 符合 GB 3304-1991 标准
 *
 * <p>
 * 标准名称：中国各民族名称的罗马字母拼写法和代码
 * <p>
 * 本类从外部JSON配置文件加载56个民族的标准代码和名称映射
 *
 * @see <a href=
 *      "http://openstd.samr.gov.cn/bzgk/gb/newGbInfo?hcno=1A79315969D93F3960652158AA86B0DC">国标委</a>
 */
public final class NationConstants {

    /** 民族代码到名称的映射 */
    private static final Map<String, String> CODE_TO_NAME;

    /** 民族名称到代码的映射 */
    private static final Map<String, String> NAME_TO_CODE;

    static {
        Map<String, String> codeToName = new HashMap<>();
        Map<String, String> nameToCode = new HashMap<>();

        try {
            loadFromConfig(codeToName, nameToCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load GB nation codes from config", e);
        }

        CODE_TO_NAME = Collections.unmodifiableMap(codeToName);
        NAME_TO_CODE = Collections.unmodifiableMap(nameToCode);
    }

    private NationConstants() {
    }

    /**
     * 从外部JSON配置文件加载民族数据
     */
    private static void loadFromConfig(Map<String, String> codeToName, Map<String, String> nameToCode)
            throws Exception {
        try (InputStream is = NationConstants.class.getResourceAsStream("/gb-nation-codes.json")) {
            if (is == null) {
                throw new RuntimeException("GB nation codes config file not found: /gb-nation-codes.json");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            JsonNode nations = root.get("nations");

            nations.forEach(node -> {
                String code = node.get("code").asText();
                String name = node.get("name").asText();
                codeToName.put(code, name);
                nameToCode.put(name, code);
            });
        }
    }

    /**
     * 验证民族代码是否有效
     *
     * @param code
     *            民族代码（01-56, 97, 98）
     * @return true 如果代码有效
     */
    public static boolean isValidCode(String code) {
        return code != null && CODE_TO_NAME.containsKey(code);
    }

    /**
     * 验证民族名称是否有效
     *
     * @param name
     *            民族名称
     * @return true 如果名称有效
     */
    public static boolean isValidName(String name) {
        return name != null && NAME_TO_CODE.containsKey(name);
    }

    /**
     * 根据代码获取民族名称
     *
     * @param code
     *            民族代码
     * @return 民族名称，如果代码无效返回null
     */
    public static String getNameByCode(String code) {
        return CODE_TO_NAME.get(code);
    }

    /**
     * 根据名称获取民族代码
     *
     * @param name
     *            民族名称
     * @return 民族代码，如果名称无效返回null
     */
    public static String getCodeByName(String name) {
        return NAME_TO_CODE.get(name);
    }

    /**
     * 获取所有民族代码到名称的映射
     *
     * @return 不可变的代码到名称映射
     */
    public static Map<String, String> getAllNations() {
        return CODE_TO_NAME;
    }
}
