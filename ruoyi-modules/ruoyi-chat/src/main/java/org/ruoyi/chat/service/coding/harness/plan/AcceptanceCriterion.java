package org.ruoyi.chat.service.coding.harness.plan;

/** A mechanically verifiable success condition. */
public record AcceptanceCriterion(
    String id,
    String type,
    String expected,
    String evidenceKey
) {

    public static final String PROCESS_EXIT_TYPE = "PROCESS_EXIT";
    public static final String PROCESS_EXIT_ZERO = "exitCode=0";
    public static final String PROCESS_CANONICAL_KEY_PREFIX = "execute_process:";
    public static final String FILE_MUTATION_TYPE = "FILE_MUTATION";
    public static final String FILE_MUTATION_SUCCESS = "success";
    public static final String FILE_CANONICAL_KEY_PREFIX = "workspace_file:";
    public static final String ACTUAL_OUTCOME_ATTRIBUTE = "actualOutcome";
    public static final String SOURCE_ASSISTANT_MESSAGE_ATTRIBUTE = "sourceAssistantMessageId";
    public static final String RESULT_MESSAGE_ATTRIBUTE = "resultMessageId";

    public AcceptanceCriterion {
        id = requireText(id, "id");
        type = requireText(type, "type");
        expected = requireText(expected, "expected");
        evidenceKey = normalizeOptional(evidenceKey);
    }

    public boolean isSatisfiedBy(ExecutionEvidence evidence) {
        boolean common = evidence != null
            && mechanicallyVerifiable()
            && evidence.successful()
            && type.equals(evidence.type())
            && evidenceKey.equals(evidence.canonicalKey())
            && expected.equals(evidence.attributes().get(ACTUAL_OUTCOME_ATTRIBUTE))
            && hasText(evidence.attributes().get(SOURCE_ASSISTANT_MESSAGE_ATTRIBUTE))
            && hasText(evidence.attributes().get(RESULT_MESSAGE_ATTRIBUTE))
            && hasText(evidence.attributes().get("toolCallId"))
            && hasText(evidence.attributes().get("sourceArgumentsDigest"));
        if (!common) {
            return false;
        }
        String toolName = evidence.attributes().get("toolName");
        String code = evidence.attributes().get("code");
        return switch (type) {
            case PROCESS_EXIT_TYPE -> "execute_process".equals(toolName)
                && "PROCESS_EXIT_ZERO".equals(code);
            case FILE_MUTATION_TYPE -> ("write_file".equals(toolName)
                || "replace_text".equals(toolName)) && "ok".equals(code);
            default -> false;
        };
    }

    /** Only combinations backed by a first-party mechanical verifier may close a criterion. */
    public boolean mechanicallyVerifiable() {
        return (PROCESS_EXIT_TYPE.equals(type) && PROCESS_EXIT_ZERO.equals(expected)
            && evidenceKey != null && evidenceKey.startsWith(PROCESS_CANONICAL_KEY_PREFIX))
            || (FILE_MUTATION_TYPE.equals(type) && FILE_MUTATION_SUCCESS.equals(expected)
            && evidenceKey != null && evidenceKey.startsWith(FILE_CANONICAL_KEY_PREFIX));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Acceptance criterion " + field + " must not be blank");
        }
        return value.strip();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
