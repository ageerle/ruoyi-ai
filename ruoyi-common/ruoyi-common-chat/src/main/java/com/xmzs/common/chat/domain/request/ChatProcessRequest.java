package com.xmzs.common.chat.domain.request;


import lombok.Data;

/**
 * @author hncboy
 * @date 2023/3/23 13:17
 * 消息处理请求
 */
@Data
public class ChatProcessRequest {

    private String prompt;

    private Options options;

    private String systemMessage;

    @Data
    public static class Options {

        private String conversationId;

        /**
         * 这里的父级消息指的是回答的父级消息 id
         * 前端发送问题，需要上下文的话传回答的父级消息 id
         */
        private String parentMessageId;
    }
}
