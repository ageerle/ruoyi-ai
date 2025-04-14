package org.ruoyi.rocket.callmcpserver.cofing;

import io.modelcontextprotocol.client.McpClient;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


/**
 * @author ageer
 */
@Configuration
public class McpClientCfg implements McpSyncClientCustomizer {


    @Override
    public void customize(String name, McpClient.SyncSpec spec) {
        // do nothing
        spec.requestTimeout(Duration.ofSeconds(30));
    }
}
