package org.ruoyi.service.coding;

/**
 * 编程能力 SSE 事件 DTO。
 *
 * <p>事件名与前端 {@code ruoyi-copilot/src/App.vue} 的 {@code applyStreamEvent} 卡片契约对齐：
 * <ul>
 *   <li>{@code thinking} / {@code text} —— LLM 思考/回复文本增量</li>
 *   <li>{@code add|edit|delete}-{start|progress|end} —— 文件写入/编辑/删除</li>
 *   <li>{@code cmd} —— 命令执行</li>
 *   <li>{@code list-progress} —— 列目录</li>
 *   <li>{@code done} / {@code error} —— 流结束</li>
 * </ul>
 *
 * <p>约束：add/edit/delete 必须带 filePath（前端用 operation.filePath 存在性区分
 * code-change 卡 vs activity-row 卡）；cmd/list-progress 不带 filePath。
 *
 * @author ageerle
 */
public record CodingSseEvent(String eventType, String filePath, String command,
                             String content, String status) {

    public static CodingSseEvent of(String eventType, String filePath, String command,
                                    String content, String status) {
        return new CodingSseEvent(eventType, filePath, command, content, status);
    }

    public static CodingSseEvent text(String content) {
        return new CodingSseEvent("text", null, null, content, null);
    }

    public static CodingSseEvent thinking(String content) {
        return new CodingSseEvent("thinking", null, null, content, null);
    }

    public static CodingSseEvent done() {
        return new CodingSseEvent("done", null, null, null, null);
    }

    public static CodingSseEvent error(String message) {
        return new CodingSseEvent("error", null, null, message, null);
    }
}
