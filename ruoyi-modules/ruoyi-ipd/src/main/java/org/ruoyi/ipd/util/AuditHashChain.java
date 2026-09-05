package org.ruoyi.ipd.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 审计日志 hash 链纯函数（v3 TS-08 / AC-AUD-01 / RISK-04 方案 A）。
 * <p>currHash = SHA256( prevHash + canonical(本条内容) )；链首 prevHash = 64 个 '0'。
 * <p>版本策略（方案 A，零破坏）：
 * <ul>
 *   <li><b>v1（现行）</b>：无版本前缀；字段序冻结；{@link #canonical} / {@link #canonicalV1} 字节兼容历史库</li>
 *   <li><b>v2（预留）</b>：显式 {@code v2|} 前缀 + 扩展段；仅当产品决定启用且写入路径切流后才可调用</li>
 * </ul>
 * 验链侧按落库当时算法重算；切 v2 后需按行标记或启发式识别版本，不得用 v2 验 v1 行。
 */
public final class AuditHashChain {

    public static final String GENESIS = "0".repeat(64);

    /** 当前写入路径使用的规范化协议版本（隐式 v1）。 */
    public static final int ACTIVE_CANONICAL_VERSION = 1;

    /** v2 规范化串显式前缀（与 v1 无前缀形成互斥命名空间）。 */
    public static final String V2_PREFIX = "v2|";

    private AuditHashChain() {
    }

    /**
     * v1 规范化（生产默认入口，保持历史调用点不变）。
     * <p>字段顺序即协议，任何插入/改序都会使历史链失效——这正是防篡改语义。
     *
     * @param seq              链序号
     * @param operatorId       操作人 ID，null→空串
     * @param operatorName     操作人姓名
     * @param operatorRole     操作人角色
     * @param action           动作码
     * @param entityType       实体类型
     * @param entityId         实体 ID
     * @param beforeData       变更前 JSON
     * @param afterData        变更后 JSON
     * @param reason           原因
     * @param createTimeMillis 创建时间毫秒
     * @return v1 规范化串（无版本前缀）
     */
    public static String canonical(Long seq, Long operatorId, String operatorName, String operatorRole,
                                   String action, String entityType, Long entityId,
                                   String beforeData, String afterData, String reason, long createTimeMillis) {
        return canonicalV1(seq, operatorId, operatorName, operatorRole, action, entityType, entityId,
            beforeData, afterData, reason, createTimeMillis);
    }

    /**
     * 显式 v1 入口（与 {@link #canonical} 字节完全一致）。
     *
     * @see #canonical
     */
    public static String canonicalV1(Long seq, Long operatorId, String operatorName, String operatorRole,
                                     String action, String entityType, Long entityId,
                                     String beforeData, String afterData, String reason, long createTimeMillis) {
        return payloadV1(seq, operatorId, operatorName, operatorRole, action, entityType, entityId,
            beforeData, afterData, reason, createTimeMillis);
    }

    /**
     * 预留 v2：在 v1 载荷前加 {@link #V2_PREFIX}，末尾追加扩展 JSON 段。
     * <p><b>禁止</b>在 ACTIVE_CANONICAL_VERSION 仍为 1 时由 {@code AuditLogService} 调用本方法写库，
     * 否则新旧行混用会导致验链策略歧义。
     *
     * @param extensionJson 扩展字段 JSON（业务自定义，null→空串）；不得回填进 v1 字段位
     * @return {@code v2|<v1-payload>|<extensionJson>}
     */
    public static String canonicalV2(Long seq, Long operatorId, String operatorName, String operatorRole,
                                     String action, String entityType, Long entityId,
                                     String beforeData, String afterData, String reason, long createTimeMillis,
                                     String extensionJson) {
        return V2_PREFIX
            + payloadV1(seq, operatorId, operatorName, operatorRole, action, entityType, entityId,
                beforeData, afterData, reason, createTimeMillis)
            + "|" + nvl(extensionJson);
    }

    /**
     * 根据版本号选择规范化实现（验链/迁移工具用）。
     *
     * @param version         1 或 2
     * @param extensionJson   仅 version=2 时使用；version=1 时忽略
     * @return 对应版本规范化串
     * @throws IllegalArgumentException 未知版本
     */
    public static String canonicalByVersion(int version, Long seq, Long operatorId, String operatorName,
                                            String operatorRole, String action, String entityType, Long entityId,
                                            String beforeData, String afterData, String reason,
                                            long createTimeMillis, String extensionJson) {
        return switch (version) {
            case 1 -> canonicalV1(seq, operatorId, operatorName, operatorRole, action, entityType, entityId,
                beforeData, afterData, reason, createTimeMillis);
            case 2 -> canonicalV2(seq, operatorId, operatorName, operatorRole, action, entityType, entityId,
                beforeData, afterData, reason, createTimeMillis, extensionJson);
            default -> throw new IllegalArgumentException("不支持的 canonical 版本: " + version);
        };
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

    /** v1 载荷本体（无版本前缀），供 v1/v2 共享字段序。 */
    private static String payloadV1(Long seq, Long operatorId, String operatorName, String operatorRole,
                                    String action, String entityType, Long entityId,
                                    String beforeData, String afterData, String reason, long createTimeMillis) {
        return seq + "|" + nvl(operatorId) + "|" + nvl(operatorName) + "|" + nvl(operatorRole)
            + "|" + nvl(action) + "|" + nvl(entityType) + "|" + nvl(entityId)
            + "|" + nvl(beforeData) + "|" + nvl(afterData) + "|" + nvl(reason) + "|" + createTimeMillis;
    }

    private static String nvl(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
