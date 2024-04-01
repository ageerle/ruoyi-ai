package com.xmzs.system.service;


import com.xmzs.common.chat.domain.request.ChatRequest;
import com.xmzs.common.chat.domain.request.Dall3Request;
import com.xmzs.common.chat.entity.images.Item;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @date 2023-04-08
 */
public interface SseService {

    /**
     * 客户端发送消息到服务端
     * @param chatRequest
     */
    SseEmitter sseChat(ChatRequest chatRequest);

    /**
     * 绘画接口
     * @param request
     */
    List<Item> dall3(Dall3Request request);


    /**
     * mj绘画接口
     */
    void mjTask();

}
