package org.ruoyi.ipd.service.ai;

/**
 * P4-2.1 AI 协议测试结果（BR-AI-PROV-05：errorCode 走白名单）。
 * <p>
 * errorMessage 仅含 host + 失败类别；绝不含 apiKey 或请求头（service 层落 AiModelView 前再次 mask）。
 *
 * @param success      是否可用
 * @param latencyMs    探测耗时（毫秒，>=0）
 * @param errorCode    失败类别（OK 时 null）：UNREACHABLE / TIMEOUT / AUTH_FAILED / HTTP_<code> / UNSUPPORTED_PROTOCOL
 * @param errorMessage 失败描述（白名单化字符串）
 */
public record AiTestResult(boolean success, long latencyMs, String errorCode, String errorMessage) {

    public static AiTestResult ok(long latencyMs) {
        return new AiTestResult(true, Math.max(0, latencyMs), null, null);
    }

    public static AiTestResult fail(String code, String message, long latencyMs) {
        return new AiTestResult(false, Math.max(0, latencyMs), code, message);
    }
}
