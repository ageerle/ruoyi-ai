package org.ruoyi.mcp.config;

import org.ruoyi.mcp.service.McpCustomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/**
 * @author ageer
 */
@Configuration
@EnableWebMvc
public class McpServerConfig implements WebMvcConfigurer {

    @Bean
    public ToolCallbackProvider openLibraryTools(McpCustomService mcpService) {
        return MethodToolCallbackProvider.builder().toolObjects(mcpService).build();
    }

    @Bean
    public List<McpServerFeatures.SyncResourceRegistration> resourceRegistrations() {

        // Create a resource registration for system information
        var systemInfoResource = new McpSchema.Resource(
                "system://info",
                "System Information",
                "Provides basic system information including Java version, OS, etc.",
                "application/json", null
        );

        var resourceRegistration = new McpServerFeatures.SyncResourceRegistration(systemInfoResource, (request) -> {
            try {
                var systemInfo = Map.of(
                        "javaVersion", System.getProperty("java.version"),
                        "osName", System.getProperty("os.name"),
                        "osVersion", System.getProperty("os.version"),
                        "osArch", System.getProperty("os.arch"),
                        "processors", Runtime.getRuntime().availableProcessors(),
                        "timestamp", System.currentTimeMillis());

                String jsonContent = new ObjectMapper().writeValueAsString(systemInfo);

                return new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", jsonContent)));
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to generate system info", e);
            }
        });

        return List.of(resourceRegistration);
    }



    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> promptRegistrations() {

        var prompt = new McpSchema.Prompt("greeting", "A friendly greeting prompt",
                List.of(new McpSchema.PromptArgument("name", "The name to greet", true)));

        var promptRegistration = new McpServerFeatures.SyncPromptRegistration(prompt, getPromptRequest -> {

            String nameArgument = (String) getPromptRequest.arguments().get("name");
            if (nameArgument == null) {
                nameArgument = "friend";
            }

            var userMessage = new McpSchema.PromptMessage(McpSchema.Role.USER,
                    new McpSchema.TextContent("Hello " + nameArgument + "! How can I assist you today?"));

            return new McpSchema.GetPromptResult("A personalized greeting message", List.of(userMessage));
        });

        return List.of(promptRegistration);
    }


    @Bean
    public Consumer<List<McpSchema.Root>> rootsChangeConsumer() {
        return roots -> {
            System.out.println("rootsChange");
        };
    }




}
