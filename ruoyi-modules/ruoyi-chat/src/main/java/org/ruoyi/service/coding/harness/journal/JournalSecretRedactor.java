package org.ruoyi.service.coding.harness.journal;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic best-effort secret redaction for durable operator-facing journals.
 *
 * <p>This intentionally runs before snapshots are retained as well as before they are rendered.
 * It is a guardrail, not a credential store: callers must still avoid putting credentials in task
 * state.</p>
 */
public final class JournalSecretRedactor {

    private static final String REDACTED = "[REDACTED]";
    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[^\\s\\\"'<>]+" );
    private static final Pattern KEY_VALUE = Pattern.compile(
        "(?i)(\\b(?:api[_-]?key|access[_-]?token|secret(?:[_-]?key)?|password|passwd|pwd|"
            + "db[_-]?password)\\b\\s*(?:=|:)\\s*)([\\\"']?)([^\\s,;\\\"'}]+)" );
    private static final Pattern JSON_SECRET = Pattern.compile(
        "(?i)(\\\"(?:api[_-]?key|access[_-]?token|secret(?:[_-]?key)?|password|passwd|pwd|"
            + "db[_-]?password)\\\"\\s*:\\s*\\\")([^\\\"]*)(\\\")" );
    private static final Pattern OPENAI_STYLE_KEY = Pattern.compile(
        "\\bsk[-_][A-Za-z0-9][A-Za-z0-9._~+/-]{7,}\\b" );
    private static final Pattern URI_PASSWORD = Pattern.compile(
        "(?i)(?:jdbc:)?[a-z][a-z0-9+.-]*://([^\\s/@:]+):([^\\s/@]+)@" );

    private JournalSecretRedactor() {
    }

    public static String redact(String value) {
        String source = Objects.requireNonNull(value, "value");
        String redacted = BEARER.matcher(source).replaceAll("Bearer " + REDACTED);
        redacted = replaceJsonSecret(redacted);
        redacted = replaceKeyValue(redacted);
        redacted = OPENAI_STYLE_KEY.matcher(redacted).replaceAll(REDACTED);
        return replaceUriPassword(redacted);
    }

    private static String replaceJsonSecret(String value) {
        Matcher matcher = JSON_SECRET.matcher(value);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(output, Matcher.quoteReplacement(
                matcher.group(1) + REDACTED + matcher.group(3)));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String replaceKeyValue(String value) {
        Matcher matcher = KEY_VALUE.matcher(value);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(output, Matcher.quoteReplacement(
                matcher.group(1) + matcher.group(2) + REDACTED));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String replaceUriPassword(String value) {
        Matcher matcher = URI_PASSWORD.matcher(value);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String whole = matcher.group();
            String replacement = whole.replace(matcher.group(2), REDACTED);
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
