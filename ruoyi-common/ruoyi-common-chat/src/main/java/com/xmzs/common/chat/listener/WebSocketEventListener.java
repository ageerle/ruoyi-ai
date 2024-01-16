package com.xmzs.common.chat.listener;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.xmzs.common.chat.constant.OpenAIConst;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import com.xmzs.common.chat.entity.chat.ChatCompletionResponse;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;

/**
 * 描述：OpenAI流式输出Socket接收
 *
 * @author https:www.unfbx.com
 * @date 2023-03-23
 */
@Slf4j
public class WebSocketEventListener extends EventSourceListener {

    private WebSocketSession session;

    /**
     * 消息结束标识
     */
    private final String msgEnd = "[DONE]";

    public WebSocketEventListener(WebSocketSession session) {
        this.session = session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(EventSource eventSource, Response response) {
        log.info("OpenAI建立Socket连接...");
    }

    /**
     * {@inheritDoc}
     */
    @SneakyThrows
    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        log.info("OpenAI返回数据：{}", data);
        if (data.equals(msgEnd)) {
            log.info("OpenAI返回数据结束了");
            session.sendMessage(new TextMessage(msgEnd));
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        // 读取Json
        ChatCompletionResponse completionResponse = mapper.readValue(data, ChatCompletionResponse.class);
        String delta = mapper.writeValueAsString(completionResponse.getChoices().get(0).getDelta());
        session.sendMessage(new TextMessage(delta));
    }


    @Override
    public void onClosed(EventSource eventSource) {
        log.info("OpenAI关闭Socket连接...");
    }


    @SneakyThrows
    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        if (Objects.isNull(response)) {
            return;
        }
        ResponseBody body = response.body();
        if (Objects.nonNull(body)) {
            // 返回非流式回复内容
            if(response.code() == OpenAIConst.SUCCEED_CODE){
                ObjectMapper mapper = new ObjectMapper();
                ChatCompletionResponse completionResponse = mapper.readValue(body.string(), ChatCompletionResponse.class);
                String delta = mapper.writeValueAsString(completionResponse.getChoices().get(0).getMessage().getContent());
                session.sendMessage(new TextMessage(delta));
            }else {
                log.error("Socket连接异常data：{}，异常：{}", body.string(), t);
            }
        } else {
            log.error("Socket连接异常data：{}，异常：{}", response, t);
        }
        eventSource.cancel();
    }
}
