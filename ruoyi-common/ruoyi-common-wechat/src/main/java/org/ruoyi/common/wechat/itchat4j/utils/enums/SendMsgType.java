package org.ruoyi.common.wechat.itchat4j.utils.enums;

import com.jfinal.plugin.activerecord.Record;

import java.util.*;
/**
 * 发送类型
 * @author WesleyOne
 * @create 2018/12/14
 */
public enum SendMsgType {

    IMG("IMG","图片"),
    FILE("FILE","文件"),
    TEXT("TEXT","纯文本")
    ;

    private String value;
    private String name;

    SendMsgType(String value, String name) {
        this.value = value;
        this.name = name;
    }


    private static final Map<String, SendMsgType> lookup = new HashMap<>();
    public static List<Record> LIST_KV = new ArrayList<>();
    static {
        for (SendMsgType s : EnumSet.allOf(SendMsgType.class)){
            lookup.put(s.toValue(), s);
            LIST_KV.add(new Record().set("v",s.toValue()).set("n",s.toName()));
        }
    }


    /**
     * 获取枚举的值（整数值、字符串值等）
     * @return
     */
    public String toValue() {
        return this.value;
    }

    public String toName() {
        return this.name;
    }


    /**
     * 根据值（整数值、字符串值等）获取相应的枚举类型
     * @param value
     * @return
     */
    public static SendMsgType fromValue(String value) {
        return lookup.get(value);
    }

    public boolean equal(SendMsgType type){
        if (type != null && this.toValue().equals(type.toValue())){
            return true;
        }
        return false;
    }
}
