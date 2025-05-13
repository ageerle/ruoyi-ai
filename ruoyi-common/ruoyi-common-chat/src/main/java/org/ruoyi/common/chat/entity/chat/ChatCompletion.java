package org.ruoyi.common.chat.entity.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

/**
 * chat模型参数
 *
 * @author https:www.unfbx.com
 * 2023-03-02
 */
@Data
@SuperBuilder
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletion extends BaseChatCompletion implements Serializable {

    /**
     * 问题描述
     */
    @NonNull
    private List<Message> messages;

}
