package org.ruoyi.common.chat.sse;

import cn.hutool.json.JSONUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.jetbrains.annotations.NotNull;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;
import org.ruoyi.common.chat.entity.chat.FunctionCall;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.openai.plugin.PluginAbstract;
import org.ruoyi.common.chat.openai.plugin.PluginParam;

import java.util.Objects;

/**
 * 插件开发返回信息收集sse监听器
 *
 * @author https:www.unfbx.com
 * 2023-08-18
 */
@Slf4j
public abstract class PluginListener<R extends PluginParam, T> extends EventSourceListener {
    /**
     * openAi插件构建的参数
     */
    private String arguments = "";
    private OpenAiStreamClient client;
    private EventSourceListener eventSourceListener;
    private PluginAbstract<R, T> plugin;
    private ChatCompletion chatCompletion;
    /**
     * 构造方法必备四个元素
     *
     * @param client              OpenAiStreamClient
     * @param eventSourceListener 处理真实第二次sse请求的自定义监听
     * @param plugin              插件信息
     * @param chatCompletion      请求参数
     */
    public PluginListener(OpenAiStreamClient client, EventSourceListener eventSourceListener, PluginAbstract<R, T> plugin, ChatCompletion chatCompletion) {
        this.client = client;
        this.eventSourceListener = eventSourceListener;
        this.plugin = plugin;
        this.chatCompletion = chatCompletion;
    }

    /**
     * 获取openAi插件构建的参数
     *
     * @return arguments
     */
    private String getArguments() {
        return this.arguments;
    }

    /**
     * sse关闭后处理，第二次请求方法
     */
    public void onClosedAfter() {
        log.debug("构造的方法值：{}", getArguments());

        R realFunctionParam = (R) JSONUtil.toBean(getArguments(), plugin.getR());
        T tq = plugin.func(realFunctionParam);

        FunctionCall functionCall = FunctionCall.builder()
                .arguments(getArguments())
                .name(plugin.getFunction())
                .build();
        chatCompletion.getMessages().add(Message.builder().role(Message.Role.ASSISTANT).content("function_call").functionCall(functionCall).build());
        chatCompletion.getMessages().add(Message.builder().role(Message.Role.FUNCTION).name(plugin.getFunction()).content(plugin.content(tq)).build());
        //设置第二次，请求的参数
        chatCompletion.setFunctionCall(null);
        chatCompletion.setFunctions(null);
        client.streamChatCompletion(chatCompletion, eventSourceListener);
    }

    @SneakyThrows
    @Override
    public final void onEvent(@NotNull EventSource eventSource, String id, String type, String data) {
        log.debug("插件开发返回信息收集sse监听器返回数据：{}", data);
        if ("[DONE]".equals(data)) {
            log.debug("插件开发返回信息收集sse监听器返回数据结束了");
            return;
        }
        ChatCompletionResponse chatCompletionResponse = JSONUtil.toBean(data, ChatCompletionResponse.class);
        if (Objects.nonNull(chatCompletionResponse.getChoices().get(0).getDelta().getFunctionCall())) {
            this.arguments += chatCompletionResponse.getChoices().get(0).getDelta().getFunctionCall().getArguments();
        }
    }

    @Override
    public final void onClosed(EventSource eventSource) {
        log.debug("插件开发返回信息收集sse监听器关闭连接...");
        this.onClosedAfter();
    }

    @Override
    public void onOpen(EventSource eventSource, Response response) {
        log.debug("插件开发返回信息收集sse监听器建立连接...");
    }

    @SneakyThrows
    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        if (Objects.isNull(response)) {
            log.error("插件开发返回信息收集sse监听器,连接异常:{}", t);
            eventSource.cancel();
            return;
        }
        ResponseBody body = response.body();
        if (Objects.nonNull(body)) {
            log.error("插件开发返回信息收集sse监听器,连接异常data：{}，异常：{}", body.string(), t);
        } else {
            log.error("插件开发返回信息收集sse监听器,连接异常data：{}，异常：{}", response, t);
        }
        eventSource.cancel();
    }
}
