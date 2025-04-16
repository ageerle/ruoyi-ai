package org.ruoyi.chat.service.chat.impl;

import cn.hutool.json.JSONUtil;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.entity.chat.ChatChoice;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;
import org.ruoyi.common.chat.entity.chat.Parameters;
import org.ruoyi.common.chat.entity.chat.tool.ToolCallFunction;
import org.ruoyi.common.chat.entity.chat.tool.Tools;
import org.ruoyi.common.chat.entity.chat.tool.ToolsFunction;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.request.ChatRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAIServiceImpl implements IChatService {

    private final OpenAiStreamClient openAiStreamClient;


    @Override
    public SseEmitter chat(ChatRequest chatRequest,SseEmitter emitter) {
        WebFluxSseClientTransport webFluxSseClientTransport = new WebFluxSseClientTransport(WebClient.builder().baseUrl("http://localhost:8080"));
        ChatCompletion completion = ChatCompletion
                .builder()
                .messages(chatRequest.getMessages())
                .model(chatRequest.getModel())
                .stream(false)
                .build();
        List<Tools> tools = new ArrayList<>();
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
            System.out.println("add a+ b =  " + sumResult.content().get(0));


            McpSchema.Content content = sumResult.content().get(0);

            emitter.send(sumResult.content().get(0));

        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        emitter.complete();
        return emitter;

    }

}
