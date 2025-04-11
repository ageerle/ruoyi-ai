package org.ruoyi.common.wechat.web.enums;

import com.jfinal.plugin.activerecord.Record;

import java.util.*;
public enum  KeyMsgValueType {

    IMG("IMG", "图片"),
    FILE("FILE", "文件"),
    TEXT("TEXT", "纯文本");

    private String value;
    private String name;

    KeyMsgValueType(String value, String name) {
        this.value = value;
        this.name = name;
    }

    private static final Map<String, KeyMsgValueType> lookup = new HashMap<>();
    public static List<Record> LIST_KV = new ArrayList<>();

    static {
        for (KeyMsgValueType s : EnumSet.allOf(KeyMsgValueType.class)) {
            lookup.put(s.toValue(), s);
            LIST_KV.add(new Record().set("v", s.toValue()).set("n", s.toName()));
        }
    }

    public String toValue() {
        return this.value;
    }

    public String toName() {
        return this.name;
    }

    public static KeyMsgValueType fromValue(String value) {
        return lookup.get(value);
    }

    public boolean equal(KeyMsgValueType type) {
        return type != null && this.toValue().equals(type.toValue());
    }
}
