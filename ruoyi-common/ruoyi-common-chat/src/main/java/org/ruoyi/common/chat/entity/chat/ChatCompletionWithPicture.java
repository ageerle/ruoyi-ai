package org.ruoyi.common.chat.entity.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

/**
 * ： chat模型附带图片的参数
 *
 * @author https:www.unfbx.com
 * @since 1.1.2
 * 2023-11-10
 */
@Data
@SuperBuilder
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionWithPicture extends BaseChatCompletion implements Serializable {
    /**
     * 问题描述
     */
    private List<MessagePicture> messages;

}
