package org.ruoyi.common.process;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Prevents provider credentials owned by the backend from crossing into ordinary child processes.
 */
public final class ChildProcessSecretSanitizer {

    public static final String DEEPSEEK_API_KEY = "DEEPSEEK_API_KEY";
    private static final Map<String, String> EMPTY_PROVIDER_SECRET_OVERRIDE =
        Map.of(DEEPSEEK_API_KEY, "");

    private ChildProcessSecretSanitizer() {
    }

    /**
     * Removes every case variant before the process is created. Environment keys are case-insensitive
     * on Windows, while removing variants is also safe on case-sensitive platforms.
     */
    public static void sanitize(ProcessBuilder builder) {
        Objects.requireNonNull(builder, "builder");
        builder.environment().keySet().removeIf(
            name -> DEEPSEEK_API_KEY.equalsIgnoreCase(name)
        );
    }

    /**
     * Keeps sanitization adjacent to {@link ProcessBuilder#start()} so a later edit cannot accidentally
     * reintroduce the inherited provider credential between configuration and process creation.
     */
    public static Process start(ProcessBuilder builder) throws IOException {
        sanitize(builder);
        return builder.start();
    }

    /**
     * LangChain4j's stdio transport only overlays its environment map on the inherited environment.
     * An explicit empty value therefore masks the backend's real credential in the MCP child.
     */
    public static Map<String, String> emptyProviderSecretOverride() {
        return EMPTY_PROVIDER_SECRET_OVERRIDE;
    }
}
