package org.ruoyi.chat.service.coding.harness.artifact;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Canonical owner/session/run namespace for an artifact. */
public record ArtifactScope(
    String ownerId,
    String sessionId,
    String runId
) {

    private static final Pattern SAFE_ID =
        Pattern.compile("[a-z0-9](?:[a-z0-9_-]{0,62}[a-z0-9])?");
    private static final Set<String> WINDOWS_RESERVED = Set.of(
        "con", "prn", "aux", "nul",
        "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
        "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
    );

    public ArtifactScope {
        ownerId = canonicalId(ownerId, "ownerId");
        sessionId = canonicalId(sessionId, "sessionId");
        runId = canonicalId(runId, "runId");
    }

    private static String canonicalId(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String canonical = value.strip().toLowerCase(Locale.ROOT);
        if (!SAFE_ID.matcher(canonical).matches() || WINDOWS_RESERVED.contains(canonical)) {
            throw new IllegalArgumentException(field + " is not a safe artifact scope id");
        }
        return canonical;
    }
}
