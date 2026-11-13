package net.jxdg.travel.common;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 敏感字段加解密与脱敏。密钥来自环境变量 AES_KEY（至少 16 位，内部取 SHA-256 前 32 字节）。
 */
@Component
public class CryptoUtil {

    private final AES aes;

    public CryptoUtil(@Value("${app.security.aes-key}") String rawKey) {
        this.aes = SecureUtil.aes(java.util.Arrays.copyOf(
                SecureUtil.sha256(rawKey).getBytes(StandardCharsets.UTF_8), 32));
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        return aes.encryptBase64(plain);
    }

    public String decrypt(String cipher) {
        if (cipher == null || cipher.isBlank()) {
            return null;
        }
        return aes.decryptStr(cipher);
    }

    /** 身份证：保留前4后4 */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "*".repeat(idCard.length() - 8) + idCard.substring(idCard.length() - 4);
    }

    /** 手机号：保留前3后4 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 密钥回掩码：只显示首尾 4 位 */
    public static String maskSecret(String secret) {
        if (secret == null || secret.length() <= 8) {
            return "********";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
