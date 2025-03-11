package org.ruoyi.common.chat.demo;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.entity.chat.tool.ToolCallFunction;
import org.ruoyi.common.chat.entity.chat.tool.ToolCalls;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * 描述： demo测试实现类，仅供思路参考
 *
 * @author https:www.unfbx.com
 * 2023-11-12
 */
@Slf4j
public class ConsoleEventSourceListenerV3 extends EventSourceListener {
    @Getter
    List<ToolCalls> choices = new ArrayList<>();
    @Getter
    ToolCalls toolCalls = new ToolCalls();
    @Getter
    ToolCallFunction toolCallFunction = ToolCallFunction.builder().name("").arguments("").build();
    final CountDownLatch countDownLatch;

    public ConsoleEventSourceListenerV3(CountDownLatch countDownLatch) {
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
            return;
        }
        ChatCompletionResponse chatCompletionResponse = JSONUtil.toBean(data, ChatCompletionResponse.class);
        Message delta = chatCompletionResponse.getChoices().get(0).getDelta();
        if (CollectionUtil.isNotEmpty(delta.getToolCalls())) {
            choices.addAll(delta.getToolCalls());
        }
    }

    @Override
    public void onClosed(EventSource eventSource) {
        if(CollectionUtil.isNotEmpty(choices)){
            toolCalls.setId(choices.get(0).getId());
            toolCalls.setType(choices.get(0).getType());
            choices.forEach(e -> {
                toolCallFunction.setName(e.getFunction().getName());
                toolCallFunction.setArguments(toolCallFunction.getArguments() + e.getFunction().getArguments());
                toolCalls.setFunction(toolCallFunction);
            });
        }
        log.info("OpenAI关闭sse连接...");
        countDownLatch.countDown();
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
