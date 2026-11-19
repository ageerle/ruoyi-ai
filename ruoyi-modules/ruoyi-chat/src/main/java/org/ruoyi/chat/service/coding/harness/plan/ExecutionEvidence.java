package org.ruoyi.chat.service.coding.harness.plan;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Immutable verifier-produced evidence. Model prose must never be converted directly into this type. */
public record ExecutionEvidence(
    String evidenceId,
    String type,
    String canonicalKey,
    String digest,
    boolean successful,
    String summary,
    Map<String, String> attributes,
    long observedAt
) {

    public ExecutionEvidence {
        evidenceId = requireText(evidenceId, "evidenceId");
        type = requireText(type, "type");
        canonicalKey = requireText(canonicalKey, "canonicalKey");
        digest = requireText(digest, "digest");
        summary = requireText(summary, "summary");
        if (observedAt <= 0) {
            throw new IllegalArgumentException("observedAt must be positive");
        }
        TreeMap<String, String> normalized = new TreeMap<>();
        if (attributes != null) {
            attributes.forEach((key, value) -> {
                if (key == null || key.isBlank() || value == null) {
                    throw new IllegalArgumentException("Evidence attributes must have non-blank keys and values");
                }
                normalized.put(key.strip(), value);
            });
        }
        attributes = Collections.unmodifiableMap(normalized);
    }

    public static ExecutionEvidence success(String type, String canonicalKey, String digest,
                                            String summary, Map<String, String> attributes,
                                            long observedAt) {
        return new ExecutionEvidence(UUID.randomUUID().toString(), type, canonicalKey, digest,
            true, summary, attributes, observedAt);
    }

    public static ExecutionEvidence failure(String type, String canonicalKey, String digest,
                                            String summary, Map<String, String> attributes,
                                            long observedAt) {
        return new ExecutionEvidence(UUID.randomUUID().toString(), type, canonicalKey, digest,
            false, summary, attributes, observedAt);
    }

    String deduplicationKey() {
        return type + "\u0000" + canonicalKey + "\u0000" + digest + "\u0000" + successful;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Execution evidence " + field + " must not be blank");
        }
        return value.strip();
    }
}
