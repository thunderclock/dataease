package io.dataease.auth.utils;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 签名工具类
 * 使用 HMAC-SHA256 算法进行签名
 */
public class SignatureUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * 生成签名
     * 
     * @param accessSecret AccessSecret
     * @param timestamp 时间戳
     * @param requestBody 请求体（JSON 字符串）
     * @return 签名字符串（Base64 编码）
     */
    public static String generateSignature(String accessSecret, Long timestamp, String requestBody) {
        if (StringUtils.isBlank(accessSecret) || timestamp == null) {
            throw new IllegalArgumentException("accessSecret and timestamp cannot be null or empty");
        }

        try {
            // 构建待签名字符串：accessSecret + timestamp + requestBody
            String signString = accessSecret + timestamp + (requestBody != null ? requestBody : "");
            
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(accessSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(signString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * 验证签名
     * 
     * @param signature 待验证的签名
     * @param accessSecret AccessSecret
     * @param timestamp 时间戳
     * @param requestBody 请求体（JSON 字符串）
     * @return 是否验证通过
     */
    public static boolean verifySignature(String signature, String accessSecret, Long timestamp, String requestBody) {
        if (StringUtils.isBlank(signature) || StringUtils.isBlank(accessSecret) || timestamp == null) {
            return false;
        }

        String expectedSignature = generateSignature(accessSecret, timestamp, requestBody);
        return signature.equals(expectedSignature);
    }
}

