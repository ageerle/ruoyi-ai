package org.ruoyi.ipd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit business fields only: never serialize a whole entity or request into an audit event. */
final class AuditEventData {
    private static final ObjectMapper JSON = new ObjectMapper();

    /** 护栏报错时回显的载荷前缀长度（避免把整段业务数据抛进异常消息/日志）。 */
    private static final int GUARD_ECHO_LIMIT = 80;

    private AuditEventData() { }

    static String json(Object... pairs) {
        if (pairs.length % 2 != 0) { throw new IllegalArgumentException("Audit fields require key/value pairs"); }
        Map<String, Object> fields = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) { fields.put((String) pairs[i], pairs[i + 1]); }
        try { return JSON.writeValueAsString(fields); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Audit fields cannot be serialized", e); }
    }

    /**
     * DEF-6 护栏：校验审计载荷是合法 JSON，非法即抛。
     *
     * <p>存在理由：{@code audit_logs.before_data/after_data} 原为 MySQL {@code json} 列，列类型
     * 本身就在 DB 层强制了 JSON 合法性——DEF-1 正是靠它 fail-fast（纯文本直写 after_data →
     * {@code MysqlDataTruncation: Invalid JSON text} → 审计与业务同事务整体回滚）。DEF-6 为让
     * hash 链能字节精确往返而把两列改 {@code longtext}，DB 不再校验 → 本方法把那道护栏补回到
     * 应用层，避免畸形载荷退化为 fail-late（静默入库、直到读取/审计导出/前端解析时才炸，
     * 且脏载荷进入 hash 链参与计算）。
     *
     * <p>异常类型故意选 {@link DataIntegrityViolationException}：与 MySQL 截断错误经 Spring
     * 翻译后的类型一致，下游全局异常处理与回滚语义保持原样（不新增异常类型，避免掉进
     * advice 白名单缺口）。注意它是 {@code DuplicateKeyException} 的父类，故调用方必须
     * <b>在重试循环之外</b>校验，否则会被当成唯一键冲突吞掉（见 {@code AuditLogService.append}）。
     *
     * @param payload 待校验载荷；{@code null} 或空串视为合法（两列可空，多数审计行无载荷）
     * @param field   列名，仅用于报错定位
     */
    static void requireJson(String payload, String field) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        try {
            JSON.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new DataIntegrityViolationException(
                "audit_logs." + field + " 不是合法 JSON（DEF-6 应用层护栏：列类型已由 json 改 longtext，"
                    + "DB 不再校验，此处复刻原 fail-fast 语义）：" + echo(payload), e);
        }
    }

    private static String echo(String payload) {
        return payload.length() <= GUARD_ECHO_LIMIT
            ? payload
            : payload.substring(0, GUARD_ECHO_LIMIT) + "...(共 " + payload.length() + " 字符)";
    }
}
