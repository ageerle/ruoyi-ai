package org.ruoyi.common.chat.security;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Resolves an explicit {@code env:NAME} model credential without exposing it through DTO JSON. */
public final class ChatModelSecretReference {

    public static final String ENV_REFERENCE_REGEXP =
        "^env:(?:(?:DEEPSEEK|PPIO)_API_KEY|" + CustomApiCredentialPolicy.REFERENCE_PATTERN + ")$";

    private static final String ENV_PREFIX = "env:";
    private static final Pattern ENV_REFERENCE = Pattern.compile(ENV_REFERENCE_REGEXP);

    private ChatModelSecretReference() {
    }

    /**
     * Package-private final resolution step. Callers must first bind provider, model and endpoint
     * through {@link ChatModelCredentialPolicy}; exposing a context-free resolver would recreate a
     * credential-confusion channel.
     */
    static String resolveAllowlistedReference(String configuredValue,
                                              Function<String, String> environment) {
        String reference = requirePersistableReference(configuredValue);
        if (reference == null) {
            return null;
        }
        String variable = reference.substring(ENV_PREFIX.length());
        String resolved = Objects.requireNonNull(environment, "environment").apply(variable);
        if (resolved == null || resolved.isBlank()) {
            throw new IllegalStateException(
                "Required model API key environment variable is not configured: " + variable);
        }
        return resolved;
    }

    /**
     * Validates the only credential representation that may cross a persistence boundary.
     * A {@code null} value means that an update did not request a credential change.
     */
    public static String requirePersistableReference(String configuredValue) {
        if (configuredValue == null) {
            return null;
        }
        if (!ENV_REFERENCE.matcher(configuredValue).matches()) {
            throw new IllegalArgumentException(
                "Invalid model API key reference; reference is not allowlisted");
        }
        return configuredValue;
    }

    /**
     * Narrow compatibility hook for an in-place legacy migration. It is deliberately package
     * private and requires an audit callback which never receives the credential value. Normal
     * provider resolution and all persistence paths must use the strict methods above.
     */
    static String resolveForLegacyMigration(String configuredValue,
                                            Function<String, String> environment,
                                            Runnable legacyPlaintextAudit) {
        if (configuredValue == null || configuredValue.startsWith(ENV_PREFIX)) {
            return resolveAllowlistedReference(configuredValue, environment);
        }
        if (configuredValue.isBlank()) {
            throw new IllegalArgumentException("Legacy model API key cannot be blank");
        }
        Objects.requireNonNull(legacyPlaintextAudit, "legacyPlaintextAudit").run();
        return configuredValue;
    }
}
