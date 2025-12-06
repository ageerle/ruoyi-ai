package org.ruoyi.common.chat.entity.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author https://www.unfbx.com
 * 2023-11-10
 */
@Data
@Builder
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class ImageUrl {
    /**
     * 图片地址，支持base64. eg: data:image/jpeg;base64,{base64_image}   <p\>
     * https://platform.openai.com/docs/guides/vision
     */
    private String url;
}
