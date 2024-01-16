package com.xmzs.common.chat.domain.request;

import com.xmzs.common.chat.entity.chat.Content;
import com.xmzs.common.chat.entity.chat.Message;
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

    @NotEmpty(message = "传入的模型不能为空")
    private String model;

    @NotEmpty(message = "对话消息不能为空")
    List<Message> messages;

    List<Content> content;

    private String prompt;

    private String userId;

    /**
     * 需要识别的图片地址
     */
    private String imgurl;

    /**
     * gpt的默认设置
     */
    private String systemMessage = "";

    private double top_p = 1;

    private double temperature = 0.2;

    /**
     * 上下文的条数
     */
    private Integer contentNumber = 10;

    /**
     * 是否携带上下文
     */
    private Boolean usingContext = Boolean.TRUE;

}
