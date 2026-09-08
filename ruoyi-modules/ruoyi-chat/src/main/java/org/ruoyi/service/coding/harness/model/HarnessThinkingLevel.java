package org.ruoyi.service.coding.harness.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Doubao Seed-Evolving 思考等级（reasoning_effort）。
 *
 * <p>none 关闭思考；其余档位按官方文档控制思考长度，默认 high。
 * 持久化与 API 传输统一使用小写名称，旧数据缺省按 high 处理。</p>
 */
public enum HarnessThinkingLevel {
    NONE("none", "关闭思考"),
    MINIMAL("minimal", "极简"),
    LOW("low", "轻快"),
    MEDIUM("medium", "均衡"),
    HIGH("high", "深入"),
    XHIGH("xhigh", "极深"),
    MAX("max", "最深");

    public static final HarnessThinkingLevel DOUBAO_DEFAULT = HIGH;

    private final String wireValue;
    private final String label;

    HarnessThinkingLevel(String wireValue, String label) {
        this.wireValue = wireValue;
        this.label = label;
    }

    /** 发送给模型的 reasoning_effort 字面值，同时也是持久化/API 传输的 JSON 值。 */
    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    /** 面向用户的中文易懂标签。 */
    public String label() {
        return label;
    }

    /** none 表示关闭思考（thinking.type=disabled，且不传 reasoning_effort）。 */
    public boolean thinkingEnabled() {
        return this != NONE;
    }

    /**
     * 解析持久化/请求中的思考等级。接受空值与未知值时回退到 Doubao 默认 high，
     * 保证旧请求、旧持久化数据缺字段可正常读取。
     */
    @JsonCreator
    public static HarnessThinkingLevel fromWire(String value) {
        if (value == null || value.isBlank()) {
            return DOUBAO_DEFAULT;
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        for (HarnessThinkingLevel level : values()) {
            if (level.wireValue.equals(normalized) || level.name().equals(normalized)) {
                return level;
            }
        }
        return DOUBAO_DEFAULT;
    }
}
