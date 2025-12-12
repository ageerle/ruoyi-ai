package org.ruoyi.mcpserve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Server 应用启动类
 * 工具通过 DynamicToolCallbackProvider 动态加载
 *
 * @author ageer
 */
@SpringBootApplication
public class RuoyiMcpServeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuoyiMcpServeApplication.class, args);
    }

}
