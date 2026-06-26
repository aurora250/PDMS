package com.pdm.common.core.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM 加密工具类。
 *
 * <p>
 * 用于敏感字段（如身份证号、手机号、密码）的加密存储和解密读取。 采用 GCM 模式提供认证加密，每次加密随机生成 12 字节 IV 并前置拼接在密文中。
 * 密钥必须通过环境变量或配置中心注入，严禁硬编码。
 * </p>
 *
 * <p>
 * 工具类不可实例化。
 * </p>
 */
public final class Aes256Utils {

    /** AES-GCM 算法标识 */
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    /** GCM 初始向量长度（12 字节） */
    private static final int GCM_IV_LENGTH = 12;
    /** GCM 认证标签长度（128 位） */
    private static final int GCM_TAG_LENGTH = 128;

    private Aes256Utils() {
    }

    /**
     * 使用 AES-256-GCM 加密明文。
     *
     * <p>
     * 输出格式：Base64(IV + 密文)，IV 为 12 字节随机数。
     * </p>
     *
     * @param plainText
     *            明文
     * @param base64Key
     *            Base64 编码的 256 位密钥
     * @return Base64 编码的密文（含 IV 前缀）
     * @throws Exception
     *             加密失败时抛出
     */
    public static String encrypt(String plainText, String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(iv);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Prepend IV to cipher text
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * 使用 AES-256-GCM 解密密文。
     *
     * @param encryptedText
     *            Base64 编码的密文（含 IV 前缀）
     * @param base64Key
     *            Base64 编码的 256 位密钥
     * @return 解密后的明文字符串
     * @throws Exception
     *             解密失败（密钥不匹配或数据损坏）时抛出
     */
    public static String decrypt(String encryptedText, String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        byte[] combined = Base64.getDecoder().decode(encryptedText);

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }
}
