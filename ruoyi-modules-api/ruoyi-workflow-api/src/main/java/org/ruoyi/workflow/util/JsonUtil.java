package org.ruoyi.workflow.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, Boolean.FALSE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModules(LocalDateTimeUtil.getSimpleModule(), new JavaTimeModule(), new Jdk8Module());
    }

    public static String toJson(Object obj) {
        String resp = null;
        try {
            resp = objectMapper.writeValueAsString(obj);
        } catch (IOException e) {
            log.error("JsonUtil error", e);
        }
        return resp;
    }


    /**
     * 创建JSON处理器的静态方法
     *
     * @param content JSON字符串
     * @return
     */
    private static JsonParser getParser(String content) {
        if (StringUtils.isNotBlank(content)) {
            try {
                return objectMapper.getFactory().createParser(content);
            } catch (IOException ioe) {
                log.error("JsonUtil getParser error", ioe);
            }
        }
        return null;
    }

    /**
     * JSON对象反序列化
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            JsonParser jp = getParser(json);
            if (null == jp) {
                log.error("json parser is null");
                return null;
            }
            return jp.readValueAs(clazz);
        } catch (IOException ioe) {
            log.error("反序列化失败", ioe);
        }
        return null;
    }

    public static <T> T fromJson(JsonNode jsonNode, Class<T> clazz) {
        try {
            return objectMapper.treeToValue(jsonNode, clazz);
        } catch (JsonProcessingException e) {
            log.error("反序列化失败", e);
        }
        return null;
    }


    public static <T> List<T> fromArrayNode(ArrayNode arrayNode, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        try {
            for (JsonNode jsonNode : arrayNode) {
                result.add(objectMapper.treeToValue(jsonNode, clazz));
            }
        } catch (JsonProcessingException e) {
            log.error("反序列化失败", e);
        }
        return result;
    }

    public static JsonNode toJsonNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("反序列化失败", e);
        }
        return null;
    }

    public static Map<String, Object> toMap(String json) {
        Map<String, Object> result;
        try {
            result = objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static <T> T toBean(String json, Class<T> tClass) {
        T result;
        try {
            result = objectMapper.readValue(json, tClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static Map<String, Object> toMap(Object obj) {
        try {
            return objectMapper.convertValue(obj, new TypeReference<HashMap<String, Object>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode classToJsonNode(Object obj) {
        return objectMapper.valueToTree(obj);
    }

    public static ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

}