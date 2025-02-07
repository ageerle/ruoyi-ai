package org.ruoyi.common.chat.entity.chat;

import lombok.*;

/**
 * 指定模型必须输出的格式的对象。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseFormat {
    /**
     * 默认：text
     *
     * @see Type
     */
    private String type;

    @Getter
    @AllArgsConstructor
    public enum Type {
        JSON_OBJECT("json_object"),
        TEXT("text"),
        ;
        private final String name;
    }
}
