package org.ruoyi.common.trace.constant;

import java.util.Map;

/**
 * 链路追踪中文展示常量。
 * <p>
 * 将技术标识映射为用户可读的中文标签，供前端直接展示。
 */
public final class TraceDisplayConstants {

    private TraceDisplayConstants() {
    }

    // ======================== 节点类型中文映射 ========================

    /**
     * 通用节点类型中文标签。
     */
    public static final Map<String, String> NODE_TYPE_LABELS = Map.ofEntries(
        Map.entry("RETRIEVAL", "知识检索"),
        Map.entry("RERANK", "重排序"),
        Map.entry("LLM_CALL", "LLM 调用"),
        Map.entry("STREAM", "流式输出"),
        Map.entry("METHOD", "方法调用"),
        Map.entry("HTTP", "HTTP 请求"),
        Map.entry("DB", "数据库"),
        Map.entry("CACHE", "缓存操作"),
        Map.entry("TASK", "异步任务"),
        Map.entry("ROOT", "根节点")
    );

    /**
     * 业务类型中文标签。
     */
    public static final Map<String, String> BUSINESS_TYPE_LABELS = Map.ofEntries(
        Map.entry("RAG_CHAT", "RAG 对话"),
        Map.entry("API", "API 调用"),
        Map.entry("SCHEDULED", "定时任务")
    );

    /**
     * 状态中文标签。
     */
    public static final Map<String, String> STATUS_LABELS = Map.ofEntries(
        Map.entry("RUNNING", "运行中"),
        Map.entry("SUCCESS", "成功"),
        Map.entry("ERROR", "失败"),
        Map.entry("CANCELLED", "已取消"),
        Map.entry("TIMEOUT", "超时")
    );

    // ======================== 工具方法 ========================

    /**
     * 获取节点类型中文标签，未匹配时返回原始值。
     */
    public static String nodeTypeLabel(String nodeType) {
        if (nodeType == null) {
            return "-";
        }
        return NODE_TYPE_LABELS.getOrDefault(nodeType.toUpperCase(), nodeType);
    }

    /**
     * 获取业务类型中文标签，未匹配时返回原始值。
     */
    public static String businessTypeLabel(String businessType) {
        if (businessType == null) {
            return "-";
        }
        return BUSINESS_TYPE_LABELS.getOrDefault(businessType.toUpperCase(), businessType);
    }

    /**
     * 获取状态中文标签，未匹配时返回原始值。
     */
    public static String statusLabel(String status) {
        if (status == null) {
            return "未知";
        }
        return STATUS_LABELS.getOrDefault(status.toUpperCase(), status);
    }

    /**
     * 将技术节点名称转为可读展示名。
     * <p>
     * 支持 kebab-case / snake_case / camelCase → 首字母大写空格分隔。
     */
    public static String prettifyNodeName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "-";
        }
        String trimmed = raw.trim();
        // 已知映射优先
        Map<String, String> known = Map.ofEntries(
            Map.entry("rag-chat", "RAG 流式对话"),
            Map.entry("rag-stream-chat", "RAG 流式对话"),
            Map.entry("retrieval-engine", "知识库检索"),
            Map.entry("multi-channel-retrieval", "多路召回"),
            Map.entry("context-build", "上下文组装"),
            Map.entry("prompt-render", "Prompt 渲染"),
            Map.entry("query-rewrite-and-split", "问题改写与拆分"),
            Map.entry("intent-resolve", "意图识别"),
            Map.entry("guidance-detect", "歧义引导"),
            Map.entry("conversation-title-gen", "会话标题生成"),
            Map.entry("user-first-packet", "用户感知首包"),
            Map.entry("llm-first-packet", "LLM 首包"),
            Map.entry("llm-chat-routing", "LLM 路由调度"),
            Map.entry("llm-stream-routing", "LLM 流式路由")
        );
        if (known.containsKey(trimmed)) {
            return known.get(trimmed);
        }
        // 通用格式化: 按 [-_] 分割，每段首字母大写
        String[] parts = trimmed.split("[-_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.length() > 0 ? sb.toString() : trimmed;
    }

    /**
     * 判断状态是否为失败。
     */
    public static boolean isFailed(String status) {
        if (status == null) {
            return false;
        }
        String upper = status.toUpperCase();
        return "ERROR".equals(upper) || "FAILED".equals(upper) || "TIMEOUT".equals(upper);
    }

    /**
     * 判断状态是否为成功。
     */
    public static boolean isSuccess(String status) {
        if (status == null) {
            return false;
        }
        return "SUCCESS".equalsIgnoreCase(status);
    }

    /**
     * 判断状态是否为运行中。
     */
    public static boolean isRunning(String status) {
        if (status == null) {
            return false;
        }
        return "RUNNING".equalsIgnoreCase(status);
    }
}
