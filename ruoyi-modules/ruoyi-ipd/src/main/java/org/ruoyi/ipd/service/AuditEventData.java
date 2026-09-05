package org.ruoyi.ipd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit business fields only: never serialize a whole entity or request into an audit event. */
final class AuditEventData {
    private static final ObjectMapper JSON = new ObjectMapper();

    private AuditEventData() { }

    static String json(Object... pairs) {
        if (pairs.length % 2 != 0) { throw new IllegalArgumentException("Audit fields require key/value pairs"); }
        Map<String, Object> fields = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) { fields.put((String) pairs[i], pairs[i + 1]); }
        try { return JSON.writeValueAsString(fields); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Audit fields cannot be serialized", e); }
    }
}
