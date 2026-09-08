package org.ruoyi.service.coding.harness.tool.command;

import java.util.Map;

/** Compatibility guard: sandboxed commands never inherit or inject host environment entries. */
final class ControlledEnvironment {

    private ControlledEnvironment() {
    }

    static Map<String, String> build(CommandToolConfig config) {
        if (!config.inheritedEnvironmentKeys().isEmpty()
            || !config.additionalEnvironment().isEmpty()) {
            throw new IllegalArgumentException(
                "Sandboxed commands cannot receive host environment entries");
        }
        return Map.of();
    }

    static void validateKeyForInheritance(String key) {
        throw new IllegalArgumentException(
            "Host environment inheritance is forbidden for sandboxed commands");
    }

    static void validateAdditionalEntry(String key, String value) {
        throw new IllegalArgumentException(
            "Host-provided container environment injection is forbidden");
    }

    static String path(Map<String, String> environment) {
        return null;
    }
}
