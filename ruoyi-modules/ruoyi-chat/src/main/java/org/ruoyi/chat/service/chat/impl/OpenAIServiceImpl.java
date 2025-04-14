package org.ruoyi.chat.service.chat.impl;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.request.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;


@Service
@Slf4j
public class OpenAIServiceImpl implements IChatService {

   private final ChatClient chatClient;

    private final ChatMemory chatMemory = new InMemoryChatMemory();


    public OpenAIServiceImpl(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
        this.chatClient = chatClientBuilder
                .defaultTools(tools)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model("gpt-4o-mini")
                                .temperature(0.4)
                                .build())
                .build();
    }

    @Override
    public SseEmitter chat(ChatRequest chatRequest,SseEmitter emitter) {
        return emitter;
    }

    @Override
    public void mcpChat(ChatRequest chatRequest, SseEmitter emitter) {
        List<Message> msgList = chatRequest.getMessages();
        // 添加记忆
        for (int i = 0; i < msgList.size(); i++) {
            org.springframework.ai.chat.messages.Message springAiMessage = new UserMessage(msgList.get(i).getContent().toString());
            chatMemory.add(String.valueOf(i), springAiMessage);
        }
        var messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory, chatRequest.getUserId().toString(), 10);

        Flux<String> content = chatClient
                .prompt(chatRequest.getPrompt())
                .advisors(messageChatMemoryAdvisor)
                .stream().content();

        content.publishOn(Schedulers.boundedElastic())
                .doOnNext(text -> {
                    try {
                        emitter.send(text);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnError(error -> {
                    log.error("Error in SSE stream: ", error);
                    emitter.completeWithError(error);
                })
                .doOnComplete(emitter::complete)
                .subscribe();
    }
}
