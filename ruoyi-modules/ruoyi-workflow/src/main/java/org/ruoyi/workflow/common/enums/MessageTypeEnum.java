package org.ruoyi.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 消息类型枚举
 *
 * @author may
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum {

    /**
     * 站内信
     */
    SYSTEM_MESSAGE("1", "站内信"),

    /**
     * 邮箱
     */
    EMAIL_MESSAGE("2", "邮箱"),

    /**
     * 短信
     */
    SMS_MESSAGE("3", "短信");

    private final String code;

    private final String desc;

    private static final Map<String, MessageTypeEnum> MESSAGE_TYPE_ENUM_MAP = Arrays.stream(values())
        .collect(Collectors.toConcurrentMap(MessageTypeEnum::getCode, Function.identity()));

    /**
     * 根据消息类型 code 获取 MessageTypeEnum
     *
     * @param code 消息类型code
     * @return MessageTypeEnum
     */
    public static MessageTypeEnum getByCode(String code) {
        return MESSAGE_TYPE_ENUM_MAP.getOrDefault(code, null);
    }

}

