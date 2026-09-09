package org.ruoyi.service.coding.harness.model;

import java.util.Locale;

/**
 * 会话验证归属模式。
 *
 * <p>AGENT（默认，旧数据）：编程智能体在 Harness 内完成独立 VERIFY，
 * 可使用 execute_process/run_inline_probe 等进程/探针工具。<br>
 * EXTERNAL：验证由独立外部验收者完成。智能体不暴露测试/进程工具，
 * 计划只绑定实际 FILE_MUTATION 证据，VERIFY 允许以一次源码/差异回顾后
 * “实现完成，等待外部验收”结束，绝不伪造测试成功或外部验收结论。</p>
 */
public enum HarnessVerificationMode {
    AGENT,
    EXTERNAL;

    public static final HarnessVerificationMode DEFAULT = AGENT;

    public static HarnessVerificationMode fromWire(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        for (HarnessVerificationMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return DEFAULT;
    }
}
