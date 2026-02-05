package org.ruoyi.service.chat.impl.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.dto.ChatMessageDTO;
import org.ruoyi.domain.dto.request.ChatRequest;
import org.ruoyi.domain.vo.chat.ChatModelVo;

import java.util.ArrayList;
import java.util.List;

/**
 * 长期记忆使用示例
 * 演示如何在实际项目中集成和使用长期记忆功能
 *
 * @author ageerle@163.com
 * @date 2025/01/10
 */
@Slf4j
public class ChatMemoryUsageExample {

    /**
     * 示例1：基础使用 - 自动长期记忆
     * <p>
     * 系统自动加载历史消息，无需手动处理
     */
    public void example1_BasicUsage() {
        log.info("=== 示例1：基础使用（自动长期记忆）===");
        /*
        // 假设已经有ChatService实例
        ChatService chatService = getBean(ChatService.class);

        // 构建请求 - 只需提供当前消息
        ChatRequest request = new ChatRequest();
        request.setSessionId(123L);  // 重要：设置会话ID以启用长期记忆
        request.setModel("gpt-4o-mini");
        request.setMessages(Arrays.asList(
            ChatMessageDTO.user("我之前告诉过你我的名字是Alice，对吧？")
        ));

        // 系统自动：
        // 1. 加载会话123的历史消息
        // 2. 合并历史消息与当前消息
        // 3. 发送给LLM
        // 4. 保存新消息到数据库和内存
        */
    }

    /**
     * 示例2：多轮对话场景
     * <p>
     * 展示长期记忆如何维护多轮对话的连续性
     */
    public void example2_MultiTurnConversation() {
        log.info("=== 示例2：多轮对话场景 ===");
        /*
        第一轮：
        User: "我是Alice，我来自纽约，我是一个数据科学家"
        AI: "很高兴认识你，Alice！听起来你在数据科学领域很专业。"
        -> 消息保存到数据库和内存

        第二轮（5分钟后）：
        User: "我现在想学习机器学习，你有什么建议吗？"
        AI: "作为一个来自纽约的数据科学家，你已经有很好的基础。..."
        -> 系统自动加载了第一轮的信息，AI能够提供更个性化的回应

        第三轮（1小时后）：
        User: "我的工作地点"
        AI: "你之前提到你来自纽约..."
        -> 即使经过很长时间，长期记忆也维持了上下文
        */
    }

    /**
     * 示例3：系统提示词与长期记忆的结合
     * <p>
     * 演示如何结合系统提示词以获得更好的效果
     */
    public void example3_SystemPromptWithMemory() {
        log.info("=== 示例3：系统提示词与长期记忆结合 ===");
        /*
        ChatRequest request = new ChatRequest();
        request.setSessionId(456L);
        request.setModel("gpt-4o-mini");

        List<ChatMessageDTO> messages = new ArrayList<>();

        // 第一条：系统角色定义
        messages.add(ChatMessageDTO.system(
            "你是一个技术助手。" +
            "你记得用户在之前的对话中分享的所有信息。" +
            "在回答时要体现出你对用户背景的了解。"
        ));

        // 第二条：当前用户消息
        messages.add(ChatMessageDTO.user("基于我之前提到的技术栈，推荐合适的框架"));

        request.setMessages(messages);

        // 系统处理流程：
        // 1. 加载会话456的历史记录（包括用户之前提到的技术栈）
        // 2. 合并系统提示词 + 历史消息 + 当前消息
        // 3. 发送完整上下文给LLM
        // 4. LLM基于完整上下文生成更准确的推荐
        */
    }

    /**
     * 示例4：清理过期消息
     * <p>
     * 展示如何手动清理特定会话的消息（例如用户要求忘记对话）
     */
    public void example4_CleanupMessages() {
        log.info("=== 示例4：清理过期消息 ===");
        /*
        // 假设已有IChatMessageService实例
        IChatMessageService chatMessageService = getBean(IChatMessageService.class);

        // 场景：用户要求"忘记我们之前的对话"
        Long sessionId = 789L;

        // 清理该会话的所有消息
        Boolean result = chatMessageService.deleteBySessionId(sessionId);

        if (result) {
            log.info("已成功清理会话 {} 的所有消息", sessionId);
            // 之后的对话将从空白状态开始，无历史上下文
        }
        */
    }

    /**
     * 示例5：消息窗口管理
     * <p>
     * 展示消息窗口机制如何防止上下文爆炸
     */
    public void example5_MessageWindowManagement() {
        log.info("=== 示例5：消息窗口管理 ===");
        /*
        配置： maxMessages = 20

        场景：用户有100条消息的历史
        会话历史: [msg1, msg2, ..., msg100]

        处理步骤：
        1. 系统从历史中加载所有消息
        2. MessageWindowChatMemory自动应用滑动窗口
        3. 只保留最近20条消息: [msg81, msg82, ..., msg100]
        4. 发送给LLM的上下文只包含20条消息

        优点：
        - 保持上下文足够充分（20条）
        - 避免超长上下文导致的高成本和低效率
        - 减少token消耗
        */
    }

    /**
     * 示例6：不同会话的隔离
     * <p>
     * 展示如何通过sessionId实现会话隔离
     */
    public void example6_SessionIsolation() {
        log.info("=== 示例6：会话隔离 ===");
        /*
        同一用户（userId = 1）有多个会话：

        会话A (sessionId = 101):
        - 讨论话题："如何学习Java"
        - 消息历史: [Q1: 学习资源, A1: 推荐书籍...]

        会话B (sessionId = 102):
        - 讨论话题："Python项目实践"
        - 消息历史: [Q1: 项目建议, A1: 推荐Django...]

        会话C (sessionId = 103):
        - 讨论话题："数据库设计"
        - 消息历史: [Q1: 选择数据库, A1: MySQL vs PostgreSQL...]

        每个会话只加载自己的历史消息，完全隔离：
        - 在会话A中，AI无法访问会话B或C的消息
        - 用户可以在不同会话间自由切换而不会混淆
        */
    }

    /**
     * 示例7：添加新的Provider实现
     * <p>
     * 如何扩展系统以支持新的LLM提供商
     */
    public void example7_AddNewProvider() {
        log.info("=== 示例7：添加新Provider ===");
        /*
        步骤：

        1. 创建新的Service类：
        @Service
        @Slf4j
        public class ClaudeServiceImpl extends AbstractStreamingChatService {

            @Override
            protected StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
                return ClaudeStreamingChatModel.builder()
                    .apiKey(chatModelVo.getApiKey())
                    .modelName(chatModelVo.getModelName())
                    .build();
            }

            @Override
            protected void doChat(ChatModelVo chatModelVo, ChatRequest chatRequest,
                                 List<ChatMessage> messages, StreamingChatResponseHandler handler) {
                // messages 已包含完整的历史上下文
                StreamingChatModel model = buildStreamingChatModel(chatModelVo, chatRequest);
                model.chat(messages, handler);
            }

            @Override
            public String getProviderName() {
                return "claude";
            }
        }

        2. 系统自动继承长期记忆能力 - 无需额外处理
        3. 所有会话数据共享同一个ChatMessage表
        */
    }

    /**
     * 示例8：自定义长期记忆行为
     * <p>
     * 展示如何在Service中自定义记忆行为
     */
    public void example8_CustomMemoryBehavior() {
        log.info("=== 示例8：自定义长期记忆行为 ===");
        /*
        在Service初始化时：

        @Service
        public class MyCustomChatService extends AbstractStreamingChatService {

            public MyCustomChatService() {
                // 禁用长期记忆（如果需要）
                setEnablePersistentMemory(false);
            }

            // 或在特定业务逻辑中
            public void processRequest(ChatRequest request) {
                if (isTemporaryChatSession(request)) {
                    setEnablePersistentMemory(false);  // 临时会话不记忆
                } else {
                    setEnablePersistentMemory(true);   // 正式会话记忆
                }
            }
        }
        */
    }

    /**
     * 示例9：配置文件使用
     * <p>
     * 展示如何通过 application.yml 配置长期记忆
     */
    public void example9_ConfigurationFile() {
        log.info("=== 示例9：配置文件使用 ===");
        /*
        application.yml 配置示例：

        chat:
          memory:
            enabled: true                    # 启用长期记忆
            maxMessages: 20                  # 消息窗口大小
            persistenceEnabled: true         # 启用数据库持久化
            autoCleanupDays: 30             # 30天后自动清理
            summarizeEnabled: false          # 禁用消息摘要
            debugLoggingEnabled: true        # 启用调试日志
            queryTimeoutMs: 5000            # 查询超时5秒
            maxConcurrentMemories: 100      # 最多100个并发内存
        */
    }

    /**
     * 示例10：监控和调试
     * <p>
     * 展示如何监控长期记忆的运行状态
     */
    public void example10_MonitoringAndDebugging() {
        log.info("=== 示例10：监控和调试 ===");
        /*
        关键日志信息：

        1. 加载历史消息：
           "已加载 15 条历史消息用于会话 12345"
           -> 表示系统成功加载了历史记录

        2. 创建失败：
           "创建聊天内存失败: ..."
           -> 需要检查数据库连接和权限

        3. 更新失败：
           "更新聊天内存失败: ..."
           -> 检查数据库是否正确配置

        4. 性能监控：
           如果加载消息时间过长，考虑：
           - 减少maxMessages值
           - 启用消息摘要
           - 定期清理过期消息
           - 为session_id列添加索引
        */
    }

    // 辅助方法
    private Object getBean(Class<?> clazz) {
        // 实现Spring Bean获取逻辑
        return null;
    }

    private boolean isTemporaryChatSession(ChatRequest request) {
        return false;
    }
}