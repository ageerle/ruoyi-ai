package org.ruoyi.chat.service.chat.impl;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.config.ChatConfig;
import org.ruoyi.chat.listener.SSEEventSourceListener;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * @author ageer
 */
@Service
@Slf4j
public class OpenAIServiceImpl implements IChatService {

    @Autowired
    private  IChatModelService chatModelService;
    @Autowired
    private ChatConfig chatConfig;
    @Autowired
    private OpenAiStreamClient openAiStreamClient;
    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ToolCallbackProvider tools;

    private final ChatMemory chatMemory = new InMemoryChatMemory();

    @Override
    public SseEmitter chat(ChatRequest chatRequest,SseEmitter emitter) {

        SSEEventSourceListener openAIEventSourceListener = new SSEEventSourceListener(emitter);
        // 查询模型信息
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());

        if(chatModelVo!=null){
            // 建请求客户端
            openAiStreamClient = chatConfig.createOpenAiStreamClient(chatModelVo.getApiHost(), chatModelVo.getApiKey());
            // 设置默认提示词
            chatRequest.setSysPrompt(chatModelVo.getSystemPrompt());
        }
        ChatCompletion completion = ChatCompletion
                .builder()
                .messages(chatRequest.getMessages())
                .model(chatRequest.getModel())
                .stream(chatRequest.getStream())
                .build();
        openAiStreamClient.streamChatCompletion(completion, openAIEventSourceListener);

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
                .options(OpenAiChatOptions.builder().model(chatRequest.getModel()).build())
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
