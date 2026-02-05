package com.example.demo.config;

import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Spring AI 配置 - 使用Spring AI 1.0.0规范
 */
@Configuration
public class SpringAIConfiguration {

    @Value("${agent.skills.dirs:Unknown}") List<Resource> agentSkillsDirs;

    @Bean
    public ChatClient chatClient(ChatModel chatModel, AppProperties appProperties) {
        // 动态获取工作目录路径
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

        return chatClientBuilder
               .defaultSystem("Always use the available skills to assist the user in their requests.")
                // Skills tool callbacks
               .defaultToolCallbacks(SkillsTool.builder().addSkillsResources(agentSkillsDirs).build())
                // Built-in tools
                .defaultTools(
                       // FileSystemTools.builder().build(),
                        ShellTools.builder().build()
                )
                .build();
    }
}
