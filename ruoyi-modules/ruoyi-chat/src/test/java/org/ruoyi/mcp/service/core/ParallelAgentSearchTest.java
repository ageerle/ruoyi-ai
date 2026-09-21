package org.ruoyi.mcp.service.core;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.ruoyi.agent.WebSearchAgent;
import org.ruoyi.domain.entity.mcp.McpTool;
import org.ruoyi.mapper.mcp.McpToolMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class ParallelAgentSearchTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void selectedRemoteToolPowersAgentSearchAndFetchWithProjectIdentity() throws Exception {
        var identities = new CopyOnWriteArrayList<String>();
        var calls = new CopyOnWriteArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", exchange -> {
            identities.add(exchange.getRequestHeaders().getFirst("User-Agent"));
            assertNull(exchange.getRequestHeaders().getFirst("Authorization"));
            assertNull(exchange.getRequestHeaders().getFirst("x-api-key"));
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders("DELETE".equals(exchange.getRequestMethod()) ? 204 : 405, -1);
                exchange.close();
                return;
            }
            var request = json.readTree(exchange.getRequestBody());
            if (!request.has("id")) {
                exchange.sendResponseHeaders(202, -1);
                exchange.close();
                return;
            }
            String result = switch (request.path("method").asText()) {
                case "initialize" -> "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{\"tools\":{}},\"serverInfo\":{\"name\":\"fixture\",\"version\":\"1\"}}";
                case "tools/list" -> "{\"tools\":[{\"name\":\"web_search\",\"inputSchema\":{\"type\":\"object\",\"properties\":{}}},{\"name\":\"web_fetch\",\"inputSchema\":{\"type\":\"object\",\"properties\":{}}}]}";
                case "tools/call" -> {
                    calls.add(request.path("params").path("name").asText());
                    yield "{\"content\":[{\"type\":\"text\",\"text\":\"https://docs.parallel.ai/integrations/mcp/search-mcp Native fixture evidence\"}],\"isError\":false}";
                }
                default -> "{}";
            };
            byte[] body = ("{\"jsonrpc\":\"2.0\",\"id\":" + request.get("id") + ",\"result\":" + result + "}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        var service = service("http://127.0.0.1:" + server.getAddress().getPort() + "/mcp");
        try {
            assertTrue(runAgent(service, "web_search").contains("Native fixture evidence"));
            assertTrue(runAgent(service, "web_fetch").contains("https://docs.parallel.ai"));
            assertEquals(List.of("web_search", "web_fetch"), calls);
            assertFalse(identities.isEmpty());
            assertTrue(identities.stream().allMatch("ruoyi-ai"::equals));
        } finally {
            service.cleanup();
            server.stop(0);
        }
    }

    @Test
    void noSelectedToolsLeavesSearchToolsUnavailable() {
        var mapper = mock(McpToolMapper.class);
        var service = new LangChain4jMcpToolProviderService(mapper, json, mock(BuiltinToolRegistry.class));
        var provider = service.getToolProvider(List.of());
        var request = new dev.langchain4j.service.tool.ToolProviderRequest("test",
            dev.langchain4j.data.message.UserMessage.from("search"));
        assertTrue(provider.provideTools(request).tools().isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUOYI_PARALLEL_LIVE", matches = "1")
    void selectedParallelConnectionReturnsHostedEvidenceThroughNativeAgent() {
        var service = service("https://search.parallel.ai/mcp");
        try {
            var search = runAgent(service, "web_search");
            var fetch = runAgent(service, "web_fetch");
            assertTrue(search.contains("https://"), search);
            assertTrue(fetch.contains("docs.parallel.ai"), fetch);
        } finally {
            service.cleanup();
        }
    }

    private LangChain4jMcpToolProviderService service(String endpoint) {
        var mapper = mock(McpToolMapper.class);
        var tool = new McpTool();
        tool.setId(41L);
        tool.setName("Parallel Search");
        tool.setType("REMOTE");
        tool.setStatus("ENABLED");
        tool.setConfigJson("{\"baseUrl\":\"" + endpoint + "\"}");
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(tool));
        return new LangChain4jMcpToolProviderService(mapper, json, mock(BuiltinToolRegistry.class));
    }

    private String runAgent(LangChain4jMcpToolProviderService service, String toolName) {
        // Deterministic tool selection exercises the production agent without a paid model.
        ChatModel router = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                var last = request.messages().get(request.messages().size() - 1);
                if (last instanceof ToolExecutionResultMessage result) {
                    assertFalse(Boolean.TRUE.equals(result.isError()), result.text());
                    return ChatResponse.builder().aiMessage(AiMessage.from(result.text())).build();
                }
                assertTrue(request.toolSpecifications().stream().anyMatch(t -> toolName.equals(t.name())));
                String args = "web_search".equals(toolName)
                    ? "{\"objective\":\"Find official Parallel Search MCP setup instructions\",\"search_queries\":[\"Parallel Search MCP official documentation\"]}"
                    : "{\"urls\":[\"https://docs.parallel.ai/integrations/mcp/search-mcp\"],\"objective\":\"Explain anonymous MCP setup\"}";
                return ChatResponse.builder().aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                    .id("call").name(toolName).arguments(args).build())).build();
            }
        };
        return AgenticServices.agentBuilder(WebSearchAgent.class)
            .chatModel(router)
            .toolProvider(service.getToolProvider(List.of(41L)))
            .build().search("请明确使用浏览器联网搜索 Parallel Search MCP 文档并注明来源");
    }
}
