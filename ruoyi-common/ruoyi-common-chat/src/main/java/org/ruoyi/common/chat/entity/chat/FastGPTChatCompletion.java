package org.ruoyi.common.chat.entity.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class FastGPTChatCompletion extends ChatCompletion implements Serializable {

    /**
     * 是否使用FastGPT提供的上下文
     */
    private String chatId;


    /**
     * 是否返回详细信息;stream模式下会通过event进行区分，非stream模式结果保存在responseData中.
     */
    private boolean detail;


    /**
     * 运行时变量
     * 模块变量，一个对象，会替换模块中，输入fastgpt框内容里的{{key}}
     */
    private Variables variables;

    /**
     * responseChatItemId: string | undefined 。
     * 如果传入，则会将该值作为本次对话的响应消息的 ID，
     * FastGPT 会自动将该 ID 存入数据库。请确保，
     * 在当前chatId下，responseChatItemId是唯一的。
     */
    private String responseChatItemId;
}
