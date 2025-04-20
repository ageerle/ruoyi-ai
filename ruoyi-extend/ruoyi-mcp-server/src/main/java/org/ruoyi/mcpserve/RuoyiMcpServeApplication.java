package org.ruoyi.mcpserve;

import org.ruoyi.mcpserve.service.ToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author ageer
 */
@SpringBootApplication
public class RuoyiMcpServeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuoyiMcpServeApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider systemTools(ToolService toolService) {
        return MethodToolCallbackProvider.builder().toolObjects(toolService).build();
    }

}
