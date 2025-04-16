package com.ivy.mcp.sse.client;


import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

public class ClientWebflux {

    public static void main(String[] args) {

        var transport = new WebFluxSseClientTransport(WebClient.builder().baseUrl("http://localhost:8080"));
        try (var client = McpClient.sync(transport).build()) {

            client.initialize();
//            client.ping();

            McpSchema.ListToolsResult toolsList = client.listTools();
            System.out.println("Available Tools = " + toolsList);

            McpSchema.CallToolResult sumResult = client.callTool(new McpSchema.CallToolRequest("add",
                    Map.of("a", 1, "b", 2)));
            System.out.println("add a+ b =  " + sumResult.content().get(0));


            McpSchema.CallToolResult currentTimResult = client.callTool(new McpSchema.CallToolRequest("getCurrentTime", Map.of()));
            System.out.println("current time Response = " + currentTimResult);
        }
    }

}
