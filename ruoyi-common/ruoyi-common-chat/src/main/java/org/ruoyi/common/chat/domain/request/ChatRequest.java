package org.ruoyi.common.chat.domain.request;

import org.ruoyi.common.chat.entity.chat.Message;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @sine 2023-04-08
 */
@Data
public class ChatRequest {


    private String frequency_penalty;

    private String max_tokens;

    @NotEmpty(message = "对话消息不能为空")
    List<Message> messages;

    @NotEmpty(message = "传入的模型不能为空")
    private String model;

    private String presence_penalty;

    private String stream;

    private double temperature;

    private double top_p = 1;

    /**
     * 知识库id
     */
    private String kid;

    private String userId;

    /**
     * 1 联网搜索
     */
    private int chat_type;

    /**
     * 应用ID
     */
    private String appId;
//

//
//    /**
//     * gpt的默认设置
//     */
//    private String systemMessage = "";
//
//
//
//    private double temperature = 0.2;
//
//    /**
//     * 上下文的条数
//     */
//    private Integer contentNumber = 10;
//
//    /**
//     * 是否携带上下文
//     */
//    private Boolean usingContext = Boolean.TRUE;

}
