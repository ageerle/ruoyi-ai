package org.ruoyi.chat.service.chat.impl;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.config.ChatConfig;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.listener.SSEEventSourceListener;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 图片识别模型
 */
@Service
@Slf4j
public class ImageOpenAiServiceImpl implements IChatService {

    @Autowired
    private IChatModelService chatModelService;

    private final ChatClient chatClient;

    public ImageOpenAiServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public SseEmitter chat(ChatRequest chatRequest, SseEmitter emitter) {
        // 从数据库获取 image 类型的模型配置
        ChatModelVo chatModelVo = chatModelService.selectModelByCategory(ChatModeType.IMAGE.getCode());
        if (chatModelVo == null) {
            log.error("未找到 image 类型的模型配置");
            emitter.completeWithError(new IllegalStateException("未找到 image 类型的模型配置"));
            return emitter;
        }

        // 创建 OpenAI 流客户端
        OpenAiStreamClient openAiStreamClient = ChatConfig.createOpenAiStreamClient(chatModelVo.getApiHost(), chatModelVo.getApiKey());
        List<Message> messages = chatRequest.getMessages();

        // 创建 SSE 事件源监听器
        SSEEventSourceListener listener = new SSEEventSourceListener(emitter, chatRequest.getUserId(), chatRequest.getSessionId());

        // 构建聊天完成请求
        ChatCompletion completion = ChatCompletion
                .builder()
                .messages(messages)
                .model(chatModelVo.getModelName()) // 使用数据库中配置的模型名称
                .stream(true)
                .build();

        // 发起流式聊天完成请求
        openAiStreamClient.streamChatCompletion(completion, listener);

        return emitter;
    }


    @Override
    public String getCategory() {
        return ChatModeType.IMAGE.getCode();
    }
}
