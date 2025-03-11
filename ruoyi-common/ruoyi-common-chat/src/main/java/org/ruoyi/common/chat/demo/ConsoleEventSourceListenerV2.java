package org.ruoyi.common.chat.demo;

import cn.hutool.json.JSONUtil;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * 描述： sse
 *
 * @author https:www.unfbx.com
 * 2023-06-15
 */
@Slf4j
public class ConsoleEventSourceListenerV2 extends EventSourceListener {
    @Getter
    String args = "";
    final CountDownLatch countDownLatch;

    public ConsoleEventSourceListenerV2(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onOpen(EventSource eventSource, Response response) {
        log.info("OpenAI建立sse连接...");
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        log.info("OpenAI返回数据：{}", data);
        if (data.equals("[DONE]")) {
            log.info("OpenAI返回数据结束了");
            countDownLatch.countDown();
            return;
        }
        ChatCompletionResponse chatCompletionResponse = JSONUtil.toBean(data, ChatCompletionResponse.class);
        if(Objects.nonNull(chatCompletionResponse.getChoices().get(0).getDelta().getFunctionCall())){
            args += chatCompletionResponse.getChoices().get(0).getDelta().getFunctionCall().getArguments();
        }
    }

    @Override
    public void onClosed(EventSource eventSource) {
        log.info("OpenAI关闭sse连接...");
    }

    @SneakyThrows
    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        if(Objects.isNull(response)){
            log.error("OpenAI  sse连接异常:{}", t);
            eventSource.cancel();
            return;
        }
        ResponseBody body = response.body();
        if (Objects.nonNull(body)) {
            log.error("OpenAI  sse连接异常data：{}，异常：{}", body.string(), t);
        } else {
            log.error("OpenAI  sse连接异常data：{}，异常：{}", response, t);
        }
        eventSource.cancel();
    }
}
