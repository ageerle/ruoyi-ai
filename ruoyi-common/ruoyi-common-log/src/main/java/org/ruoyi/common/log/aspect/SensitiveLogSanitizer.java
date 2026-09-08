package org.ruoyi.common.log.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.ruoyi.common.web.utils.SafeRequestLogUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Recursively removes credential fields from operation-audit JSON. */
final class SensitiveLogSanitizer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "oldpassword", "newpassword", "confirmpassword", "passwd", "pwd",
        "apikey", "clientsecret", "secret", "secretkey", "accesskey", "privatekey",
        "token", "tokenid", "accesstoken", "refreshtoken", "authorization", "credential",
        "credentials", "dbpassword"
    );

    private SensitiveLogSanitizer() {
    }

    static String sanitizeJson(String json, String... additionalSensitiveFields) {
        if (json == null || json.isBlank()) {
            return json;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode safeRoot = redact(root, normalizedFields(additionalSensitiveFields));
            return MAPPER.writeValueAsString(safeRoot);
        } catch (Exception ignored) {
            // Never fall back to the raw value: it may be exactly the malformed secret payload.
            return SafeRequestLogUtils.sanitizeText(json);
        }
    }

    private static Set<String> normalizedFields(String[] additionalSensitiveFields) {
        Set<String> fields = new HashSet<>(SENSITIVE_FIELDS);
        if (additionalSensitiveFields != null) {
            Arrays.stream(additionalSensitiveFields)
                .filter(value -> value != null && !value.isBlank())
                .map(SensitiveLogSanitizer::normalize)
                .forEach(fields::add);
        }
        return fields;
    }

    private static JsonNode redact(JsonNode node, Set<String> sensitiveFields) {
        if (node == null) {
            return null;
        }
        if (node instanceof ObjectNode object) {
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (sensitiveFields.contains(normalize(field.getKey()))) {
                    fields.remove();
                } else {
                    JsonNode value = field.getValue();
                    if (value != null && value.isTextual()) {
                        object.set(field.getKey(), TextNode.valueOf(
                            SafeRequestLogUtils.sanitizeText(value.textValue())));
                    } else {
                        object.set(field.getKey(), redact(value, sensitiveFields));
                    }
                }
            }
        } else if (node instanceof ArrayNode array) {
            for (int index = 0; index < array.size(); index++) {
                JsonNode value = array.get(index);
                if (value != null && value.isTextual()) {
                    array.set(index, TextNode.valueOf(
                        SafeRequestLogUtils.sanitizeText(value.textValue())));
                } else {
                    array.set(index, redact(value, sensitiveFields));
                }
            }
        } else if (node.isTextual()) {
            return TextNode.valueOf(SafeRequestLogUtils.sanitizeText(node.textValue()));
        }
        return node;
    }

    private static String normalize(String fieldName) {
        return fieldName.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }
}
