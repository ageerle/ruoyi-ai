package org.ruoyi.common.web.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ruoyi.common.core.utils.StringUtils;

import java.lang.reflect.Array;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fail-closed boundary for values that may be written to HTTP logs.
 */
public final class SafeRequestLogUtils {

    public static final String REDACTED = "[REDACTED]";

    private static final int MAX_URI_LENGTH = 512;
    private static final int MAX_TEXT_LENGTH = 4096;
    private static final int MAX_DEPTH = 16;
    private static final int MAX_COLLECTION_ITEMS = 100;
    private static final String TRUNCATED = "[TRUNCATED]";
    private static final String CYCLE = "[CYCLE]";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> SECRET_KEYS = Set.of(
        "token", "tokenid", "accesstoken", "refreshtoken", "authorization",
        "apikey", "clientsecret", "password", "passwd", "pwd", "secret",
        "secretkey", "secretaccesskey", "sessiontoken", "jwtsecretkey",
        "signingkey", "signature", "privatekey", "credential", "credentials"
    );
    private static final Pattern ONLINE_TOKEN_PATH = Pattern.compile(
        "(?i)(/monitor/online/(?:myself/)?)([^/?#;\\s]+)");
    private static final Pattern MYSELF_TOKEN_PATH = Pattern.compile(
        "(?i)(/myself/)([^/?#;\\s]+)");
    private static final Pattern GENERIC_SECRET_PATH = Pattern.compile(
        "(?i)(/(?:token(?:[-_]?id)?|access[-_]?token|refresh[-_]?token|api[-_]?key)/)"
            + "([^/?#;\\s]+)");
    private static final String SECRET_FIELD =
        "(?:[a-z0-9_-]*(?:token|api[_-]?key|secret|password|passwd|pwd|credential|"
            + "authorization|signing[_-]?key|signature)[a-z0-9_-]*)";
    private static final Pattern JSON_SECRET = Pattern.compile(
        "(?i)(\\\"" + SECRET_FIELD + "\\\"\\s*:\\s*)"
            + "(?:\\\"(?:\\\\.|[^\\\"])*\\\"|'(?:\\\\.|[^'])*'|"
            + "\\[[^\\]]*\\]|[^,}\\r\\n]*(?=[,}\\r\\n]|$))");
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
        "(?i)(\\b" + SECRET_FIELD + "\\b\\s*(?:=|:)\\s*)"
            + "(?:\\\"(?:\\\\.|[^\\\"])*\\\"|'(?:\\\\.|[^'])*'|"
            + "[^&,;}\\r\\n]*?(?=\\s+[a-z0-9_-]+\\s*(?:=|:)|[&,;}\\r\\n]|$))");
    private static final Pattern AUTH_SCHEME = Pattern.compile(
        "(?i)\\b(?:Bearer|Basic)\\s+[^\\s\\\"'<>]+");
    private static final Pattern JWT = Pattern.compile(
        "\\beyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}(?:\\.[A-Za-z0-9_-]{5,})?\\b");
    private static final Pattern OPENAI_STYLE_KEY = Pattern.compile(
        "\\bsk[-_][A-Za-z0-9][A-Za-z0-9._~+/-]{7,}\\b");
    private static final Pattern URI_PASSWORD = Pattern.compile(
        "(?i)((?:jdbc:)?[a-z][a-z0-9+.-]*://[^\\s/@:]+:)([^\\s/@]+)(@)");

    private SafeRequestLogUtils() {
    }

    /**
     * Keeps only the path portion and removes query/fragment data entirely.
     */
    public static String sanitizeUri(String uri) {
        if (StringUtils.isBlank(uri)) {
            return uri;
        }
        int suffixIndex = firstNonNegative(uri.indexOf('?'), uri.indexOf('#'));
        String safe = suffixIndex >= 0 ? uri.substring(0, suffixIndex) : uri;
        safe = ONLINE_TOKEN_PATH.matcher(safe).replaceAll("$1" + REDACTED);
        safe = MYSELF_TOKEN_PATH.matcher(safe).replaceAll("$1" + REDACTED);
        safe = GENERIC_SECRET_PATH.matcher(safe).replaceAll("$1" + REDACTED);
        safe = sanitizeUnstructuredText(safe);
        return abbreviate(safe, MAX_URI_LENGTH);
    }

    /**
     * Sanitizes structured JSON recursively and falls back to bounded text patterns.
     */
    public static String sanitizeText(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (looksLikeJson(trimmed)) {
            try {
                Object parsed = OBJECT_MAPPER.readValue(trimmed, Object.class);
                return abbreviate(writeSanitized(parsed), MAX_TEXT_LENGTH);
            } catch (JsonProcessingException ignored) {
                // Invalid JSON is still protected by the unstructured boundary below.
            }
        }
        return abbreviate(sanitizeUnstructuredText(value), MAX_TEXT_LENGTH);
    }

    /**
     * Recursively sanitizes maps, iterables, arrays, POJOs and JSON-compatible values.
     */
    public static String sanitizeObject(Object value) {
        if (value == null) {
            return null;
        }
        return abbreviate(writeSanitized(value), MAX_TEXT_LENGTH);
    }

    private static String writeSanitized(Object value) {
        Object safe = sanitizeValue(value, null, 0, new IdentityHashMap<>());
        if (safe instanceof String text) {
            return text;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(safe);
        } catch (JsonProcessingException ignored) {
            return "[UNSERIALIZABLE:" + safeType(value) + "]";
        }
    }

    private static Object sanitizeValue(Object value, String fieldName, int depth,
                                        IdentityHashMap<Object, Boolean> seen) {
        if (isSecretKey(fieldName)) {
            return REDACTED;
        }
        if (value == null || value instanceof Number || value instanceof Boolean
            || value instanceof Character || value instanceof Enum<?>) {
            return value;
        }
        if (value instanceof CharSequence text) {
            return sanitizeUnstructuredText(text.toString());
        }
        if (value instanceof Throwable failure) {
            return "[THROWABLE:" + failure.getClass().getName() + "]";
        }
        if (value instanceof Date || value instanceof TemporalAccessor) {
            return value.toString();
        }
        if (depth >= MAX_DEPTH) {
            return TRUNCATED;
        }
        if (seen.put(value, Boolean.TRUE) != null) {
            return CYCLE;
        }
        try {
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> safe = new LinkedHashMap<>();
                int count = 0;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (count++ >= MAX_COLLECTION_ITEMS) {
                        safe.put(TRUNCATED, TRUNCATED);
                        break;
                    }
                    String key = safeMapKey(entry.getKey());
                    safe.put(key, sanitizeValue(entry.getValue(), key, depth + 1, seen));
                }
                return safe;
            }
            if (value instanceof Iterable<?> iterable) {
                List<Object> safe = new ArrayList<>();
                int count = 0;
                for (Object item : iterable) {
                    if (count++ >= MAX_COLLECTION_ITEMS) {
                        safe.add(TRUNCATED);
                        break;
                    }
                    safe.add(sanitizeValue(item, null, depth + 1, seen));
                }
                return safe;
            }
            if (value.getClass().isArray()) {
                List<Object> safe = new ArrayList<>();
                int length = Math.min(Array.getLength(value), MAX_COLLECTION_ITEMS);
                for (int index = 0; index < length; index++) {
                    safe.add(sanitizeValue(Array.get(value, index), null, depth + 1, seen));
                }
                if (Array.getLength(value) > length) {
                    safe.add(TRUNCATED);
                }
                return safe;
            }
            try {
                Object converted = OBJECT_MAPPER.convertValue(value, Object.class);
                if (converted == value || converted == null) {
                    return "[OBJECT:" + safeType(value) + "]";
                }
                return sanitizeValue(converted, fieldName, depth + 1, seen);
            } catch (IllegalArgumentException | StackOverflowError ignored) {
                return "[OBJECT:" + safeType(value) + "]";
            }
        } finally {
            seen.remove(value);
        }
    }

    private static String sanitizeUnstructuredText(String value) {
        String safe = AUTH_SCHEME.matcher(value).replaceAll("[AUTH " + REDACTED + "]");
        safe = JSON_SECRET.matcher(safe).replaceAll("$1\"" + REDACTED + "\"");
        safe = KEY_VALUE_SECRET.matcher(safe).replaceAll("$1" + REDACTED);
        safe = JWT.matcher(safe).replaceAll(REDACTED);
        safe = OPENAI_STYLE_KEY.matcher(safe).replaceAll(REDACTED);
        safe = URI_PASSWORD.matcher(safe).replaceAll("$1" + REDACTED + "$3");
        return safe;
    }

    private static boolean isSecretKey(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return SECRET_KEYS.contains(normalized)
            || normalized.contains("token")
            || normalized.contains("authorization")
            || normalized.contains("apikey")
            || normalized.contains("secret")
            || normalized.contains("signingkey")
            || normalized.contains("signature")
            || normalized.contains("password")
            || normalized.contains("passwd")
            || normalized.contains("credential");
    }

    private static String safeMapKey(Object key) {
        if (key == null) {
            return "null";
        }
        if (key instanceof CharSequence || key instanceof Number || key instanceof Enum<?>) {
            return sanitizeUnstructuredText(String.valueOf(key));
        }
        return "[KEY:" + safeType(key) + "]";
    }

    private static boolean looksLikeJson(String value) {
        return (value.startsWith("{") && value.endsWith("}"))
            || (value.startsWith("[") && value.endsWith("]"));
    }

    private static int firstNonNegative(int left, int right) {
        if (left < 0) {
            return right;
        }
        if (right < 0) {
            return left;
        }
        return Math.min(left, right);
    }

    private static String safeType(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    private static String abbreviate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength) + "...(truncated)";
    }
}
