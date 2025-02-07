package org.ruoyi.common.chat.entity.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 描述：
 *
 * @author https://www.unfbx.com
 * @since 1.1.2
 * 2023-11-10
 */
@Data
@Builder
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    /**
     * 输入类型：text、image_url
     *
     * @see Type
     */
    private String type;
    private String text;
    @JsonProperty("image_url")
    private ImageUrl imageUrl;

    /**
     * 生成图片风格
     */
    @Getter
    @AllArgsConstructor
    public enum Type {
        TEXT("text"),
        IMAGE_URL("image_url"),
        ;
        private final String name;
    }
}
