package org.ruoyi.ipd.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 审计日志 hash 链纯函数（v3 TS-08 / AC-AUD-01）
 * currHash = SHA256( prevHash + canonical(本条内容) )；链首 prevHash = 64 个 '0'。
 * 规范化串必须跨版本稳定：字段顺序固定、null 记作空串、时间取毫秒值。
 */
public final class AuditHashChain {

    public static final String GENESIS = "0".repeat(64);

    private AuditHashChain() {
    }

    /** 规范化本条内容（字段顺序即协议，任何变更都会使历史链失效——这正是防篡改语义） */
    public static String canonical(Long seq, Long operatorId, String operatorName, String operatorRole,
                                   String action, String entityType, Long entityId,
                                   String beforeData, String afterData, String reason, long createTimeMillis) {
        return seq + "|" + nvl(operatorId) + "|" + nvl(operatorName) + "|" + nvl(operatorRole)
            + "|" + nvl(action) + "|" + nvl(entityType) + "|" + nvl(entityId)
            + "|" + nvl(beforeData) + "|" + nvl(afterData) + "|" + nvl(reason) + "|" + createTimeMillis;
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    public static String computeCurrHash(String prevHash, String canonical) {
        return sha256Hex(nvl(prevHash) + canonical);
    }

    private static String nvl(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}