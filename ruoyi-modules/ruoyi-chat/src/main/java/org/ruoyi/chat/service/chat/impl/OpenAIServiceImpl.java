package org.ruoyi.chat.service.chat.impl;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.listener.SSEEventSourceListener;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.request.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;


@Service
@Slf4j
public class OpenAIServiceImpl implements IChatService {

    @Autowired
    private OpenAiStreamClient openAiStreamClient;

    private final ChatClient chatClient;

    public OpenAIServiceImpl(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.chatClient = chatClientBuilder
                .defaultOptions(
                        OpenAiChatOptions.builder().model("gpt-4o-mini").build())
                .defaultTools(new SyncMcpToolCallbackProvider(mcpSyncClients))
                .build();
    }

    @Override
    public SseEmitter chat(ChatRequest chatRequest,SseEmitter emitter) {
        String toolString = mcpChat(chatRequest.getPrompt());
        Message userMessage = Message.builder().content("工具返回信息："+toolString).role(Message.Role.USER).build();
        List<Message> messages = chatRequest.getMessages();
        messages.add(userMessage);
        SSEEventSourceListener listener = new SSEEventSourceListener(emitter);
        ChatCompletion completion = ChatCompletion
                .builder()
                .messages(messages)
                .model(chatRequest.getModel())
                .stream(true)
                .build();
        openAiStreamClient.streamChatCompletion(completion, listener);
        return emitter;
    }

    public String mcpChat(String prompt){
        return this.chatClient.prompt(prompt).call().content();
    }

}
