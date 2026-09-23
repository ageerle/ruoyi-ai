package org.ruoyi.mcp.service.core;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.agent.WebSearchAgent;
import org.ruoyi.domain.entity.mcp.McpTool;
import org.ruoyi.mapper.mcp.McpToolMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class RemoteMcpBearerKeyTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void selectedRemoteToolSendsBearerKeyDuringDiscoveryAndCall() throws Exception {
        List<RequestHeader> requests = runAgentWithRemoteTool("synthetic-key");
        assertTrue(requests.stream().anyMatch(r -> "initialize".equals(r.method())));
        assertTrue(requests.stream().anyMatch(r -> "tools/list".equals(r.method())));
        assertTrue(requests.stream().anyMatch(r -> "tools/call".equals(r.method())));
        assertTrue(requests.stream().allMatch(r -> "Bearer synthetic-key".equals(r.authorization())));
    }

    @Test
    void remoteToolWithoutBearerKeyRemainsAnonymous() throws Exception {
        List<RequestHeader> requests = runAgentWithRemoteTool(null);
        assertTrue(requests.stream().anyMatch(r -> "tools/call".equals(r.method())));
        assertTrue(requests.stream().allMatch(r -> r.authorization() == null));
    }

    private List<RequestHeader> runAgentWithRemoteTool(String bearerKey) throws Exception {
        List<RequestHeader> requests = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            var request = json.readTree(exchange.getRequestBody());
            requests.add(new RequestHeader(request.path("method").asText(),
                exchange.getRequestHeaders().getFirst("Authorization")));
            if (!request.has("id")) {
                exchange.sendResponseHeaders(202, -1);
                exchange.close();
                return;
            }
            String result = switch (request.path("method").asText()) {
                case "initialize" -> "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{\"tools\":{}},\"serverInfo\":{\"name\":\"fixture\",\"version\":\"1\"}}";
                case "tools/list" -> "{\"tools\":[{\"name\":\"fixture_search\",\"inputSchema\":{\"type\":\"object\",\"properties\":{}}}]}";
                case "tools/call" -> "{\"content\":[{\"type\":\"text\",\"text\":\"fixture result\"}],\"isError\":false}";
                default -> "{}";
            };
            byte[] body = ("{\"jsonrpc\":\"2.0\",\"id\":" + request.get("id") + ",\"result\":" + result + "}")
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        McpTool tool = new McpTool();
        tool.setId(41L);
        tool.setName("Fixture remote search");
        tool.setType("REMOTE");
        tool.setStatus("ENABLED");
        ObjectNode config = json.createObjectNode()
            .put("baseUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/mcp");
        if (bearerKey != null) {
            config.put("bearerKey", bearerKey);
        }
        tool.setConfigJson(json.writeValueAsString(config));
        McpToolMapper mapper = mock(McpToolMapper.class);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(tool));
        var service = new LangChain4jMcpToolProviderService(mapper, json, mock(BuiltinToolRegistry.class));
        try {
            ChatModel router = new ChatModel() {
                @Override
                public ChatResponse doChat(ChatRequest request) {
                    var last = request.messages().get(request.messages().size() - 1);
                    if (last instanceof ToolExecutionResultMessage result) {
                        return ChatResponse.builder().aiMessage(AiMessage.from(result.text())).build();
                    }
                    assertTrue(request.toolSpecifications().stream()
                        .anyMatch(spec -> "fixture_search".equals(spec.name())));
                    return ChatResponse.builder().aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                        .id("call").name("fixture_search").arguments("{}").build())).build();
                }
            };
            String answer = AgenticServices.agentBuilder(WebSearchAgent.class)
                .chatModel(router)
                .toolProvider(service.getToolProvider(List.of(41L)))
                .build()
                .search("Use the selected search tool");
            assertTrue(answer.contains("fixture result"));
        } finally {
            service.cleanup();
            server.stop(0);
        }
        return requests.stream().filter(r -> !r.method().startsWith("notifications/")).toList();
    }

    private record RequestHeader(String method, String authorization) {
    }
}
