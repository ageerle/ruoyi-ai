package org.ruoyi.mcp.tools;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行审批令牌存储
 * 使用内存存储短期有效令牌，用于“先计划后执行”闭环
 */
public final class ApprovalTokenStore {

    private static final Map<String, TokenRecord> TOKENS = new ConcurrentHashMap<>();

    private ApprovalTokenStore() {
    }

    public static String issue(String goal, long ttlSeconds) {
        cleanupExpired();
        String token = UUID.randomUUID().toString();
        long expireAt = Instant.now().getEpochSecond() + Math.max(60, ttlSeconds);
        TOKENS.put(token, new TokenRecord(goal == null ? "" : goal, expireAt));
        return token;
    }

    public static boolean consume(String token, String scope) {
        if (token == null || token.isBlank()) {
            return false;
        }
        TokenRecord record = TOKENS.remove(token.trim());
        if (record == null) {
            return false;
        }
        if (record.expireAtEpochSeconds() < Instant.now().getEpochSecond()) {
            return false;
        }
        String normalizedScope = scope == null ? "" : scope.trim();
        return record.goal().equals(normalizedScope);
    }

    private static void cleanupExpired() {
        long now = Instant.now().getEpochSecond();
        TOKENS.entrySet().removeIf(entry -> entry.getValue().expireAtEpochSeconds() < now);
    }

    private record TokenRecord(String goal, long expireAtEpochSeconds) {
    }
}
