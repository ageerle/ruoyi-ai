package org.ruoyi.observability;

import dev.langchain4j.Experimental;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import dev.langchain4j.observability.api.AiServiceListenerRegistrar;
import dev.langchain4j.observability.api.listener.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * LangChain4j 可观测性配置类。
 * <p>
 * 负责注册所有 langchain4j 的监听器：
 * <ul>
 *   <li>{@link AiServiceListener} - AI服务级别的事件监听器（通过 AiServiceListenerRegistrar 注册）</li>
 *   <li>{@link ChatModelListener} - ChatModel 级别的监听器（注入到模型构建器）</li>
 *   <li>{@link EmbeddingModelListener} - EmbeddingModel 级别的监听器（注入到模型构建器）</li>
 * </ul>
 *
 * @author evo
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class LangChain4jObservabilityConfig {

    private final AiServiceListenerRegistrar registrar = AiServiceListenerRegistrar.newInstance();

    /**
     * 注册 AI 服务级别的事件监听器
     */
    @PostConstruct
    public void registerAiServiceListeners() {
        log.info("正在注册 LangChain4j AI Service 事件监听器...");
        registrar.register(
            new MyAiServiceStartedListener(),
            new MyAiServiceRequestIssuedListener(),
            new MyAiServiceResponseReceivedListener(),
            new MyAiServiceCompletedListener(),
            new MyAiServiceErrorListener(),
            new MyInputGuardrailExecutedListener(),
            new MyOutputGuardrailExecutedListener(),
            new MyToolExecutedEventListener()
        );
        log.info("LangChain4j AI Service 事件监听器注册完成");
    }

    // ==================== AI Service 监听器 Beans ====================

    @Bean
    public AiServiceStartedListener aiServiceStartedListener() {
        return new MyAiServiceStartedListener();
    }

    @Bean
    public AiServiceRequestIssuedListener aiServiceRequestIssuedListener() {
        return new MyAiServiceRequestIssuedListener();
    }

    @Bean
    public AiServiceResponseReceivedListener aiServiceResponseReceivedListener() {
        return new MyAiServiceResponseReceivedListener();
    }

    @Bean
    public AiServiceCompletedListener aiServiceCompletedListener() {
        return new MyAiServiceCompletedListener();
    }

    @Bean
    public AiServiceErrorListener aiServiceErrorListener() {
        return new MyAiServiceErrorListener();
    }

    @Bean
    public InputGuardrailExecutedListener inputGuardrailExecutedListener() {
        return new MyInputGuardrailExecutedListener();
    }

    @Bean
    public OutputGuardrailExecutedListener outputGuardrailExecutedListener() {
        return new MyOutputGuardrailExecutedListener();
    }

    @Bean
    public ToolExecutedEventListener toolExecutedEventListener() {
        return new MyToolExecutedEventListener();
    }

    // ==================== ChatModel 监听器 ====================

    @Bean
    public ChatModelListener chatModelListener() {
        return new MyChatModelListener();
    }

    @Bean
    public List<ChatModelListener> chatModelListeners() {
        return List.of(new MyChatModelListener());
    }

    // ==================== EmbeddingModel 监听器 ====================

    @Bean
    @Experimental
    public EmbeddingModelListener embeddingModelListener() {
        return new MyEmbeddingModelListener();
    }

    @Bean
    @Experimental
    public List<EmbeddingModelListener> embeddingModelListeners() {
        return List.of(new MyEmbeddingModelListener());
    }

    // ==================== MCP Client 监听器 ====================

    @Bean
    public McpClientListener mcpClientListener() {
        return new MyMcpClientListener();
    }
}
