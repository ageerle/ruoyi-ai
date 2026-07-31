package org.ruoyi.service.shortdrama.support;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 增量 JSON 数组解析器：随 LLM 流式输出累积 buffer，逐个提取已闭合的顶层对象，
 * 每完成一个就解析成目标类型并返回（已返回的不再重复）。
 * 用于分镜规划流式推送——第一个 panel 解析出来即可展示，不等整个数组完成。
 * <p>
 * 仅依赖 brace matching + 字符串/转义状态机，对未闭合的尾对象不解析，保证稳定性。
 */
@Slf4j
public final class IncrementalJsonArrayExtractor<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Class<T> type;
    private final StringBuilder buf = new StringBuilder();
    private int emittedCount = 0;
    /** 数组起始 '[' 在 buffer 中的位置，-1 表示尚未找到 */
    private int arrayStart = -1;

    public IncrementalJsonArrayExtractor(Class<T> type) {
        this.type = type;
    }

    /**
     * 喂入新的累积文本，返回本次新解析出的完整对象（去重）。
     * 调用方应每次把"完整 buffer"传入（实现内部不重复追加，而是以最新 buffer 为准）。
     */
    public List<T> feed(String fullBuffer) {
        List<T> newly = new ArrayList<>();
        if (StrUtil.isBlank(fullBuffer)) return newly;
        buf.setLength(0);
        buf.append(fullBuffer);

        if (arrayStart < 0) {
            arrayStart = findArrayStart(fullBuffer);
            if (arrayStart < 0) return newly;
        }

        int scanFrom = arrayStart + 1;
        int objIdx = 0;
        int i = scanFrom;
        int len = buf.length();
        while (i < len) {
            char c = buf.charAt(i);
            if (c == '{') {
                int end = findObjectEnd(i);
                if (end < 0) break; // 对象未闭合，等后续 token
                if (objIdx >= emittedCount) {
                    String objJson = buf.substring(i, end + 1);
                    T parsed = tryParse(objJson);
                    if (parsed != null) {
                        newly.add(parsed);
                        emittedCount++;
                    }
                }
                i = end + 1;
                objIdx++;
            } else if (c == ']') {
                break; // 数组结束
            } else if (Character.isWhitespace(c)) {
                i++;
            } else {
                i++;
            }
        }
        return newly;
    }

    /** 定位第一个 '['（跳过思考文字、markdown 代码块围栏 ```json 等）。 */
    private int findArrayStart(String s) {
        return s.indexOf('[');
    }

    /**
     * 从 start（指向 '{'）开始，找到该对象的闭合 '}'，正确处理字符串、转义、嵌套。
     * 返回闭合 '}' 的索引；若未闭合返回 -1。
     */
    private int findObjectEnd(int start) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < buf.length(); i++) {
            char c = buf.charAt(i);
            if (escape) { escape = false; continue; }
            if (inString) {
                if (c == '\\') { escape = true; }
                else if (c == '"') { inString = false; }
                continue;
            }
            if (c == '"') { inString = true; }
            else if (c == '{') { depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private T tryParse(String json) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            log.debug("增量解析 panel 失败，跳过: {}", e.getMessage());
            return null;
        }
    }
}
