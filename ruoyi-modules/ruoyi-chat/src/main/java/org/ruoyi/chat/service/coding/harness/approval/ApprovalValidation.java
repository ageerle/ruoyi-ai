package org.ruoyi.chat.service.coding.harness.approval;

import java.util.regex.Pattern;

final class ApprovalValidation {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private ApprovalValidation() {
    }

    static String requireId(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 256) {
            throw new IllegalArgumentException(field + " must be a non-blank bounded id");
        }
        String normalized = value.strip();
        for (int i = 0; i < normalized.length(); i++) {
            char character = normalized.charAt(i);
            if (Character.isISOControl(character)) {
                throw new IllegalArgumentException(field + " must not contain control characters");
            }
        }
        return normalized;
    }

    static String requireSha256(String value) {
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "argumentsSha256 must be an exact lowercase SHA-256 digest");
        }
        return value;
    }

    static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 2_048) {
            throw new IllegalArgumentException("Approval note exceeds its limit");
        }
        return normalized;
    }
}
