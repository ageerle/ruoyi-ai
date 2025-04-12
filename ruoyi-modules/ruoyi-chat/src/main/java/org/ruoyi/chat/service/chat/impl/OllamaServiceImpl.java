package org.ruoyi.chat.service.chat.impl;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.chat.OllamaChatRequestModel;
import io.github.ollama4j.models.generate.OllamaStreamHandler;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.chat.util.SSEUtil;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Service
@Slf4j
public class OllamaServiceImpl implements IChatService {

    @Autowired
    private  IChatModelService chatModelService;
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private ToolCallbackProvider tools;

    private final ChatMemory chatMemory = new InMemoryChatMemory();


    @Override
    public SseEmitter chat(ChatRequest chatRequest,SseEmitter emitter) {
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        String host = chatModelVo.getApiHost();
        List<Message> msgList = chatRequest.getMessages();

        List<OllamaChatMessage> messages = new ArrayList<>();
        for (Message message : msgList) {
            OllamaChatMessage ollamaChatMessage = new OllamaChatMessage();
            ollamaChatMessage.setRole(OllamaChatMessageRole.USER);
            ollamaChatMessage.setContent(message.getContent().toString());
            messages.add(ollamaChatMessage);
        }
        OllamaAPI api = new OllamaAPI(host);
        api.setRequestTimeoutSeconds(100);
        OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(chatRequest.getModel());

        OllamaChatRequestModel requestModel = builder
                .withMessages(messages)
                .build();

        // 异步执行 OllAma API 调用
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder response = new StringBuilder();
                OllamaStreamHandler streamHandler = (s) -> {
                    String substr = s.substring(response.length());
                    response.append(substr);
                    System.out.println(substr);
                    try {
                        emitter.send(substr);
                    } catch (IOException e) {
                        SSEUtil.sendErrorEvent(emitter, e.getMessage());
                    }
                };
                api.chat(requestModel, streamHandler);
                emitter.complete();
            } catch (Exception e) {
                SSEUtil.sendErrorEvent(emitter, e.getMessage());
            }
        });

        return emitter;
    }

    @Override
    public SseEmitter mcpChat(ChatRequest chatRequest, SseEmitter emitter) {
        List<Message> msgList = chatRequest.getMessages();
        // 添加记忆
        for (int i = 0; i < msgList.size(); i++) {
            org.springframework.ai.chat.messages.Message springAiMessage = new UserMessage(msgList.get(i).getContent().toString());
            chatMemory.add(String.valueOf(i),springAiMessage);
        }
        var messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory, chatRequest.getUserId(), 10);

        this.chatClient.prompt(chatRequest.getPrompt())
                .advisors(messageChatMemoryAdvisor)
                .tools(tools)
                .options(OllamaOptions.builder()
                        .model(OllamaModel.QWEN_2_5_7B)
                        .temperature(0.4)
                        .build())
                .stream()
                .chatResponse()
                .subscribe(
                        chatResponse -> {
                            try {
                                emitter.send(chatResponse, MediaType.APPLICATION_JSON);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        },
                        error -> {
                            try {
                                emitter.completeWithError(error);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        },
                        () -> {
                            try {
                                emitter.complete();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                );

        return emitter;
    }
}
