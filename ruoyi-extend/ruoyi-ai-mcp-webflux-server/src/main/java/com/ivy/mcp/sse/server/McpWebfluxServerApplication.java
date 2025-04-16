package com.ivy.mcp.sse.server;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
public class McpWebfluxServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpWebfluxServerApplication.class, args);
    }


    @Bean
    public List<ToolCallback> tools(MyTools myTools) {
        return List.of(ToolCallbacks.from(myTools));
    }

    @Service
    public static class MyTools {

        @Tool(description = "add two numbers")
        public Integer add(@ToolParam(description = "first number") int a,
                           @ToolParam(description = "second number") int b) {

            return a + b;
        }

        @Tool(description = "get current time")
        public LocalDateTime getCurrentTime() {
            return LocalDateTime.now();
        }
    }
}
