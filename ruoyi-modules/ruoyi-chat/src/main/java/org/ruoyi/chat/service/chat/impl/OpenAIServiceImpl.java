package org.ruoyi.chat.service.chat.impl;

import cn.hutool.json.JSONUtil;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.listener.SSEEventSourceListener;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.entity.chat.*;
import org.ruoyi.common.chat.entity.chat.tool.ToolCallFunction;
import org.ruoyi.common.chat.entity.chat.tool.Tools;
import org.ruoyi.common.chat.entity.chat.tool.ToolsFunction;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.common.core.exception.ServiceException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class OpenAIServiceImpl implements IChatService {

    @Autowired
    private OpenAiStreamClient openAiStreamClient;

    private final ChatClient chatClient;

    private final ChatMemory chatMemory = new InMemoryChatMemory();



    public OpenAIServiceImpl(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients, ToolCallbackProvider tools) {
        this.chatClient = chatClientBuilder
                .defaultTools(new SyncMcpToolCallbackProvider(mcpSyncClients))
                .build();
    }


    public String webMcpChat(String prompt){
        return this.chatClient.prompt(prompt).call().content();

    }


    @Override
    public SseEmitter chat(ChatRequest chatRequest,SseEmitter emitter) {
        String toolString = webMcpChat(chatRequest.getPrompt());

        Message userMessage = Message.builder().content("工具返回信息："+toolString).role(Message.Role.ASSISTANT).build();
        List<Message> messages = chatRequest.getMessages();
        messages.add(userMessage);

        SSEEventSourceListener openAIEventSourceListener = new SSEEventSourceListener(emitter);
        ChatCompletion completion = ChatCompletion
                .builder()
                .messages(messages)
                .model(chatRequest.getModel())
                .stream(true)
                .build();
        openAiStreamClient.streamChatCompletion(completion, openAIEventSourceListener);
        return emitter;

    }

    public String mcpChat(ChatRequest chatRequest) {
        WebFluxSseClientTransport webFluxSseClientTransport = new WebFluxSseClientTransport(WebClient.builder().baseUrl("http://localhost:8080"));
        ChatCompletion completion = ChatCompletion
                .builder()
                .messages(chatRequest.getMessages())
                .model(chatRequest.getModel())
                .stream(false)
                .build();
        List<Tools> tools = new ArrayList<>();
        McpSchema.Content content = null;
        try (var client = McpClient.sync(webFluxSseClientTransport).build()) {
            client.initialize();
            McpSchema.ListToolsResult toolsList = client.listTools();

            for (McpSchema.Tool mcpTool : toolsList.tools()) {

                McpSchema.JsonSchema jsonSchema = mcpTool.inputSchema();

                Parameters parameters = Parameters.builder()
                        .type(jsonSchema.type())
                        .properties(jsonSchema.properties())
                        .required(jsonSchema.required()).build();

                Tools tool = Tools.builder()
                        .type(Tools.Type.FUNCTION.getName())
                        .function(ToolsFunction.builder().name(mcpTool.name()).description(mcpTool.description()).parameters(parameters).build())
                        .build();
                tools.add(tool);
            }
            completion.setTools(tools);
            ChatCompletionResponse chatCompletionResponse = openAiStreamClient.chatCompletion(completion);
            String arguments = chatCompletionResponse.getChoices().get(0).getMessage().getToolCalls().get(0).getFunction().getArguments();
            String name = chatCompletionResponse.getChoices().get(0).getMessage().getToolCalls().get(0).getFunction().getName();
            Map<String, Object> map = JSONUtil.toBean(arguments, Map.class);
            McpSchema.CallToolResult sumResult = client.callTool(new McpSchema.CallToolRequest(name, map));
            content= sumResult.content().get(0);
        } catch (Exception e) {
           throw new ServiceException("请求失败"+e);
        }
        return content.toString();
    }

}
