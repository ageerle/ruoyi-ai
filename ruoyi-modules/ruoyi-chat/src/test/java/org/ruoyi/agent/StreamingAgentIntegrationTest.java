package org.ruoyi.agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruoyi.observability.OutputChannel;
import org.ruoyi.observability.StreamingOutputWrapper;
import org.ruoyi.observability.SupervisorStreamListener;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 子 Agent 流式输出集成测试
 *
 * 测试内容：
 * 1. 观察单个 Agent 的流式输出
 * 2. 观察 Supervisor 调用子 Agent 的流式输出
 * 3. 验证 AgentListener 事件回调
 * 4. 验证 StreamingOutputWrapper 的 token 拦截
 *
 * 注意：运行测试前需要配置正确的 API Key
 * 可以通过环境变量或直接修改配置区域
 *
 * @author ageerle@163.com
 * @date 2025/04/10
 */
@Disabled("需要配置 API Key 后手动启用")
public class StreamingAgentIntegrationTest {

    // ==================== 配置区域 ====================
    private static final String BASE_URL = "https://api.ppio.com/openai";
    private static final String API_KEY = System.getenv("PPIO_API_KEY") != null
        ? System.getenv("PPIO_API_KEY")
        : "xx"; // 默认 Key
    private static final String MODEL_NAME = "deepseek/deepseek-v3.2";

    private StreamingChatModel streamingModel;
    private OpenAiChatModel syncModel;

    // ==================== Agent 接口定义 ====================

    public interface MathAgent {
        @SystemMessage("你是一个数学计算助手，帮助用户解决数学问题。直接给出计算结果和简要解释。")
        @UserMessage("计算：{{query}}")
        @dev.langchain4j.agentic.Agent("数学计算助手")
        String calculate(@V("query") String query);
    }

    public interface TextAgent {
        @SystemMessage("你是一个文本分析助手，帮助用户分析文本内容。给出简洁的分析结果。")
        @UserMessage("分析以下文本：{{text}}")
        @dev.langchain4j.agentic.Agent("文本分析助手")
        String analyze(@V("text") String text);
    }

    public interface WeatherAgent {
        @SystemMessage("你是一个天气助手。根据用户提供的信息给出天气相关的回答。")
        @UserMessage("回答问题：{{query}}")
        @dev.langchain4j.agentic.Agent("天气助手")
        String answer(@V("query") String query);
    }

    // ==================== 初始化 ====================

    @BeforeEach
    void setUp() {
//        streamingModel = OpenAiStreamingChatModel.builder()
//            .baseUrl(BASE_URL)
//            .apiKey(API_KEY)
//            .modelName(MODEL_NAME)
//            .build();

        streamingModel = OpenAiStreamingChatModel.builder()
            .baseUrl(BASE_URL)
            .apiKey(API_KEY)
            .listeners(List.of(new ChatModelListener() {
                @Override
                public void onRequest(ChatModelRequestContext ctx) {
                    // 请求发送前
                }
                @Override
                public void onResponse(ChatModelResponseContext ctx) {
                    // 响应完成后
                }
                @Override
                public void onError(ChatModelErrorContext ctx) {
                    // 错误时
                }
            }))
            .build();


        syncModel = OpenAiChatModel.builder()
            .baseUrl(BASE_URL)
            .apiKey(API_KEY)
            .modelName(MODEL_NAME)
            .build();
    }

    // ==================== 测试方法 ====================

    @Test
    @DisplayName("测试1: 基础流式输出 - 单个 Agent")
    void testBasicStreamingAgent() throws Exception {
        System.out.println("\n=== 测试1: 基础流式输出 - 单个 Agent ===\n");

        // 创建事件总线
        String requestId = UUID.randomUUID().toString();
        OutputChannel channel = OutputChannel.create(requestId);
        CountDownLatch completed = new CountDownLatch(1);

        // 包装模型以捕获流式输出
        ChatModel wrappedModel = new StreamingOutputWrapper(streamingModel, channel);

        // 构建 Agent
        MathAgent mathAgent = AgenticServices.agentBuilder(MathAgent.class)
            .chatModel(wrappedModel)
            .build();

        // 异步执行
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println(">>> 调用 MathAgent.calculate()...");
                String result = mathAgent.calculate("计算 123 * 456 + 789 的值");
                System.out.println("\n>>> 最终结果: " + result);
            } catch (Exception e) {
                System.err.println(">>> 异常: " + e.getMessage());
                channel.completeWithError(e);
            } finally {
                channel.complete();
                completed.countDown();
            }
        });

        // drain 推送
        channel.drain(text -> {
            System.out.print(text);
            System.out.flush();
        });

        completed.await(30, TimeUnit.SECONDS);
        OutputChannel.remove(requestId);
    }

    @Test
    @DisplayName("测试2: Supervisor 模式 - 单个子 Agent 流式输出")
    void testSupervisorWithSingleSubAgent() throws Exception {
        System.out.println("\n=== 测试2: Supervisor 模式 - 单个子 Agent ===\n");

        String requestId = UUID.randomUUID().toString();
        OutputChannel channel = OutputChannel.create(requestId);
        CountDownLatch completed = new CountDownLatch(1);

        // 包装模型

        // 子 Agent
        MathAgent mathAgent = AgenticServices.agentBuilder(MathAgent.class)
            .streamingChatModel(streamingModel)
            .build();


        // Supervisor（注册监听器）
        SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
            .chatModel(syncModel)
            //.listener(new SupervisorStreamListener(channel))
            .subAgents(mathAgent)
            .responseStrategy(SupervisorResponseStrategy.LAST)
            .build();

        // 异步执行
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println(">>> Supervisor.invoke() 开始...");
                String result = supervisor.invoke("帮我计算 999 除以 3 等于多少");
                System.out.println("\n>>> Supervisor 结果: " + result);
            } catch (Exception e) {
                System.err.println(">>> 异常: " + e.getMessage());
                e.printStackTrace();
               // channel.completeWithError(e);
            } finally {
              //  channel.complete();
                completed.countDown();
            }
        });

        // drain 推送
        channel.drain(text -> {
            System.out.print(text);
            System.out.flush();
        });

        completed.await(60, TimeUnit.SECONDS);
        OutputChannel.remove(requestId);
    }

    @Test
    @DisplayName("测试3: Supervisor 模式 - 多个子 Agent 流式输出")
    void testSupervisorWithMultipleSubAgents() throws Exception {
        System.out.println("\n=== 测试3: Supervisor 模式 - 多个子 Agent ===\n");

        String requestId = UUID.randomUUID().toString();
        OutputChannel channel = OutputChannel.create(requestId);
        CountDownLatch completed = new CountDownLatch(1);

        // 包装模型
        ChatModel wrappedModel = new StreamingOutputWrapper(streamingModel, channel);


        // 子 Agent
        MathAgent mathAgent = AgenticServices.agentBuilder(MathAgent.class)
            .chatModel(wrappedModel)
            .build();

        TextAgent textAgent = AgenticServices.agentBuilder(TextAgent.class)
            .chatModel(wrappedModel)
            .build();

        WeatherAgent weatherAgent = AgenticServices.agentBuilder(WeatherAgent.class)
            .chatModel(wrappedModel)
            .build();

        // Supervisor
        SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
            .chatModel(syncModel)
            .listener(new SupervisorStreamListener(channel))
            .subAgents(mathAgent, textAgent, weatherAgent)
            .responseStrategy(SupervisorResponseStrategy.LAST)
            .build();

        // 异步执行 - 提一个会触发多个 Agent 的问题
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println(">>> Supervisor.invoke() 开始...");
                String result = supervisor.invoke(
                    "请帮我做两件事：1. 计算 50 * 20 的结果；2. 分析 '人工智能正在改变世界' 这句话的含义"
                );
                System.out.println("\n>>> Supervisor 结果: " + result);
            } catch (Exception e) {
                System.err.println(">>> 异常: " + e.getMessage());
                e.printStackTrace();
                channel.completeWithError(e);
            } finally {
                channel.complete();
                completed.countDown();
            }
        });

        // drain 推送 - 实时观察流式输出
        channel.drain(text -> {
            System.out.print("观察流式输出："+text);
            System.out.flush();
        });

        completed.await(90, TimeUnit.SECONDS);
        OutputChannel.remove(requestId);
    }

    @Test
    @DisplayName("测试4: 直接观察 StreamingChatModel 的流式响应")
    void testDirectStreamingChatModel() throws Exception {
        System.out.println("\n=== 测试4: 直接观察 StreamingChatModel ===\n");

        StringBuilder buffer = new StringBuilder();
        CountDownLatch completed = new CountDownLatch(1);

        streamingModel.chat("你好，请自我介绍", new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                buffer.append(partialResponse);
                System.out.print(partialResponse);
                System.out.flush();
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("\n\n[完成] 总Token数: " +
                    (completeResponse.metadata() != null && completeResponse.metadata().tokenUsage() != null
                        ? completeResponse.metadata().tokenUsage().totalTokenCount()
                        : "无"));
                completed.countDown();
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("[错误] " + error.getMessage());
                completed.countDown();
            }
        });

        completed.await(30, TimeUnit.SECONDS);
        System.out.println("完整响应内容: " + buffer.toString());
    }
}
