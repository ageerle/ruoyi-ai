package com.example.demo.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

/**
 * JSON Schema definition class
 * Used to define tool parameter structure and validation rules
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonSchema {
    
    private String type;
    private String description;
    private String pattern;
    private Number minimum;
    private Number maximum;
    private List<Object> enumValues;
    
    @JsonProperty("properties")
    private Map<String, JsonSchema> properties;
    
    @JsonProperty("required")
    private List<String> requiredFields;
    
    @JsonProperty("items")
    private JsonSchema items;

    // Constructor
    public JsonSchema() {}

    // Static factory methods
    public static JsonSchema object() {
        JsonSchema schema = new JsonSchema();
        schema.type = "object";
        schema.properties = new HashMap<>();
        return schema;
    }

    public static JsonSchema string(String description) {
        JsonSchema schema = new JsonSchema();
        schema.type = "string";
        schema.description = description;
        return schema;
    }

    public static JsonSchema number(String description) {
        JsonSchema schema = new JsonSchema();
        schema.type = "number";
        schema.description = description;
        return schema;
    }

    public static JsonSchema integer(String description) {
        JsonSchema schema = new JsonSchema();
        schema.type = "integer";
        schema.description = description;
        return schema;
    }

    public static JsonSchema bool(String description) {
        JsonSchema schema = new JsonSchema();
        schema.type = "boolean";
        schema.description = description;
        return schema;
    }

    public static JsonSchema array(JsonSchema items) {
        JsonSchema schema = new JsonSchema();
        schema.type = "array";
        schema.items = items;
        return schema;
    }

    public static JsonSchema array(String description, JsonSchema items) {
        JsonSchema schema = new JsonSchema();
        schema.type = "array";
        schema.description = description;
        schema.items = items;
        return schema;
    }

    // Fluent methods
    public JsonSchema addProperty(String name, JsonSchema property) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.put(name, property);
        return this;
    }

    public JsonSchema required(String... fields) {
        this.requiredFields = Arrays.asList(fields);
        return this;
    }

    public JsonSchema pattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public JsonSchema minimum(Number minimum) {
        this.minimum = minimum;
        return this;
    }

    public JsonSchema maximum(Number maximum) {
        this.maximum = maximum;
        return this;
    }

    public JsonSchema enumValues(Object... values) {
        this.enumValues = Arrays.asList(values);
        return this;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public Number getMinimum() { return minimum; }
    public void setMinimum(Number minimum) { this.minimum = minimum; }

    public Number getMaximum() { return maximum; }
    public void setMaximum(Number maximum) { this.maximum = maximum; }

    public List<Object> getEnumValues() { return enumValues; }
    public void setEnumValues(List<Object> enumValues) { this.enumValues = enumValues; }

    public Map<String, JsonSchema> getProperties() { return properties; }
    public void setProperties(Map<String, JsonSchema> properties) { this.properties = properties; }

    public List<String> getRequiredFields() { return requiredFields; }
    public void setRequiredFields(List<String> requiredFields) { this.requiredFields = requiredFields; }

    public JsonSchema getItems() { return items; }
    public void setItems(JsonSchema items) { this.items = items; }
}
