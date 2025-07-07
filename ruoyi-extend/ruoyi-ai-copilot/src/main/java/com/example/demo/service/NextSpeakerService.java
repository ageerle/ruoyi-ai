package com.example.demo.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能判断下一步发言者的服务
 */
@Service
public class NextSpeakerService {

    private static final Logger logger = LoggerFactory.getLogger(NextSpeakerService.class);
    
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public NextSpeakerService(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
    }

    private static final String CHECK_PROMPT = """
        Analyze *only* the content and structure of your immediately preceding response (your last turn in the conversation history).
        Based *strictly* on that response, determine who should logically speak next: the 'user' or the 'model' (you).

        **Decision Rules (apply in order):**
        1. **Model Continues:** If your last response explicitly states an immediate next action *you* intend to take
           (e.g., "Next, I will...", "Now I'll process...", "Moving on to analyze...", "Let me create...", "I'll now...",
           indicates an intended tool call that didn't execute), OR if the response seems clearly incomplete
           (cut off mid-thought without a natural conclusion), then the **'model'** should speak next.
        2. **Question to User:** If your last response ends with a direct question specifically addressed *to the user*,
           then the **'user'** should speak next.
        3. **Waiting for User:** If your last response completed a thought, statement, or task *and* does not meet
           the criteria for Rule 1 (Model Continues) or Rule 2 (Question to User), it implies a pause expecting
           user input or reaction. In this case, the **'user'** should speak next.

        **Output Format:**
        Respond *only* in JSON format. Do not include any text outside the JSON structure.
        """;

    // 简化版本的检查提示，用于减少LLM调用开销
    private static final String SIMPLIFIED_CHECK_PROMPT = """
        Based on your last response, who should speak next: 'user' or 'model'?
        Rules: If you stated a next action or response is incomplete -> 'model'.
        If you asked user a question -> 'user'.
        If task completed -> 'user'.
        Respond in JSON: {"next_speaker": "user/model", "reasoning": "brief reason"}
        """;

    /**
     * 判断下一步应该由谁发言 - 优化版本，添加快速路径
     */
    public NextSpeakerResponse checkNextSpeaker(List<Message> conversationHistory) {
        try {
            // 确保有对话历史
            if (conversationHistory.isEmpty()) {
                return new NextSpeakerResponse("user", "No conversation history available");
            }

            // 获取最后一条消息
            Message lastMessage = conversationHistory.get(conversationHistory.size() - 1);

            // 如果最后一条不是助手消息，用户应该发言
            if (!(lastMessage instanceof AssistantMessage)) {
                return new NextSpeakerResponse("user", "Last message was not from assistant");
            }

            // 检查是否是空响应
            String lastContent = lastMessage.getText();
            if (lastContent == null || lastContent.trim().isEmpty()) {
                return new NextSpeakerResponse("model", "Last message was empty, model should continue");
            }

            // 快速路径1: 使用增强的内容分析进行快速判断
            NextSpeakerResponse fastPathResult = performFastPathCheck(lastContent);
            if (fastPathResult != null) {
                logger.debug("Fast path decision: {}", fastPathResult);
                return fastPathResult;
            }

            // 快速路径2: 检查对话历史模式
            NextSpeakerResponse patternResult = checkConversationPattern(conversationHistory);
            if (patternResult != null) {
                logger.debug("Pattern-based decision: {}", patternResult);
                return patternResult;
            }

            // 只有在快速路径无法确定时才使用LLM判断
            logger.debug("Fast paths inconclusive, falling back to LLM check");
            return performLLMCheck(conversationHistory);

        } catch (Exception e) {
            logger.warn("Failed to check next speaker, defaulting to user", e);
            return new NextSpeakerResponse("user", "Error occurred during check: " + e.getMessage());
        }
    }

    /**
     * 快速路径检查 - 基于内容分析的快速判断
     */
    private NextSpeakerResponse performFastPathCheck(String lastContent) {
        String lowerContent = lastContent.toLowerCase();

        // 明确的停止信号 - 直接返回user
        String[] definiteStopSignals = {
            "task completed successfully",
            "all files created successfully",
            "project setup complete",
            "website is ready",
            "application is ready",
            "everything is ready",
            "setup is complete",
            "all tasks completed",
            "work is complete"
        };

        for (String signal : definiteStopSignals) {
            if (lowerContent.contains(signal)) {
                return new NextSpeakerResponse("user", "Fast path: Definite completion signal detected");
            }
        }

        // 明确的继续信号 - 直接返回model
        String[] definiteContinueSignals = {
            "next, i will",
            "now i will",
            "let me create",
            "let me edit",
            "let me update",
            "i'll create",
            "i'll edit",
            "i'll update",
            "moving on to",
            "proceeding to",
            "next step is to"
        };

        for (String signal : definiteContinueSignals) {
            if (lowerContent.contains(signal)) {
                return new NextSpeakerResponse("model", "Fast path: Definite continue signal detected");
            }
        }

        // 工具调用成功模式 - 通常需要继续
        if (isToolCallSuccessPattern(lastContent)) {
            // 但如果同时包含完成信号，则停止
            if (containsCompletionSignal(lowerContent)) {
                return new NextSpeakerResponse("user", "Fast path: Tool success with completion signal");
            }
            return new NextSpeakerResponse("model", "Fast path: Tool call success, should continue");
        }

        // 直接问用户问题 - 应该等待用户回答
        if (lowerContent.trim().endsWith("?") && containsUserQuestion(lowerContent)) {
            return new NextSpeakerResponse("user", "Fast path: Direct question to user");
        }

        return null; // 无法快速判断
    }

    /**
     * 检查对话历史模式
     */
    private NextSpeakerResponse checkConversationPattern(List<Message> conversationHistory) {
        if (conversationHistory.size() < 2) {
            return null;
        }

        // 检查最近几轮的模式
        int recentTurns = Math.min(4, conversationHistory.size());
        int modelTurns = 0;
        int userTurns = 0;

        for (int i = conversationHistory.size() - recentTurns; i < conversationHistory.size(); i++) {
            Message msg = conversationHistory.get(i);
            if (msg instanceof AssistantMessage) {
                modelTurns++;
            } else if (msg instanceof UserMessage) {
                userTurns++;
            }
        }

        // 如果模型连续说话太多轮，可能需要用户介入
        if (modelTurns >= 3 && userTurns == 0) {
            return new NextSpeakerResponse("user", "Pattern: Too many consecutive model turns");
        }

        return null; // 模式不明确
    }

    /**
     * 检查是否包含完成信号
     */
    private boolean containsCompletionSignal(String lowerContent) {
        String[] completionSignals = {
            "all done", "complete", "finished", "ready", "that's it",
            "we're done", "task complete", "project complete"
        };

        for (String signal : completionSignals) {
            if (lowerContent.contains(signal)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否包含对用户的直接问题
     */
    private boolean containsUserQuestion(String lowerContent) {
        String[] userQuestionPatterns = {
            "what would you like",
            "what do you want",
            "would you like me to",
            "do you want me to",
            "should i",
            "would you prefer",
            "any preferences",
            "what's next",
            "what should i do next"
        };

        for (String pattern : userQuestionPatterns) {
            if (lowerContent.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private NextSpeakerResponse performLLMCheck(List<Message> conversationHistory) {
        try {
            long startTime = System.currentTimeMillis();

            // 优化：只使用最近的对话历史，减少上下文长度
            List<Message> recentHistory = getRecentHistory(conversationHistory, 6); // 最多6条消息

            // 创建用于判断的对话历史 - 简化版本
            List<Message> checkMessages = recentHistory.stream()
                .map(msg -> {
                    if (msg instanceof UserMessage) {
                        // 截断过长的用户消息
                        String text = msg.getText();
                        if (text.length() > 500) {
                            text = text.substring(0, 500) + "...";
                        }
                        return new UserMessage(text);
                    } else if (msg instanceof AssistantMessage) {
                        // 截断过长的助手消息
                        String text = msg.getText();
                        if (text.length() > 500) {
                            text = text.substring(0, 500) + "...";
                        }
                        return new AssistantMessage(text);
                    }
                    return msg;
                })
                .collect(java.util.stream.Collectors.toList());

            // 添加简化的检查提示
            checkMessages.add(new UserMessage(SIMPLIFIED_CHECK_PROMPT));

            // 使用输出转换器
            BeanOutputConverter<NextSpeakerResponse> outputConverter =
                new BeanOutputConverter<>(NextSpeakerResponse.class);

            // 调用LLM - 这里可以考虑添加超时，但Spring AI目前不直接支持
            ChatResponse response = ChatClient.create(chatModel)
                .prompt()
                .messages(checkMessages)
                .call()
                .chatResponse();

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("LLM check completed in {}ms", duration);

            String responseText = response.getResult().getOutput().getText();
            logger.debug("Next speaker check response: {}", responseText);

            // 解析响应
            try {
                return outputConverter.convert(responseText);
            } catch (Exception parseError) {
                logger.warn("Failed to parse next speaker response, trying manual parsing", parseError);
                return parseManually(responseText);
            }

        } catch (Exception e) {
            logger.warn("LLM check failed, defaulting to user: {}", e.getMessage());
            return new NextSpeakerResponse("user", "LLM check failed: " + e.getMessage());
        }
    }

    /**
     * 获取最近的对话历史
     */
    private List<Message> getRecentHistory(List<Message> fullHistory, int maxMessages) {
        if (fullHistory.size() <= maxMessages) {
            return fullHistory;
        }

        return fullHistory.subList(fullHistory.size() - maxMessages, fullHistory.size());
    }

    private NextSpeakerResponse parseManually(String responseText) {
        try {
            // 简单的手动解析
            if (responseText.toLowerCase().contains("\"next_speaker\"") && 
                responseText.toLowerCase().contains("\"model\"")) {
                return new NextSpeakerResponse("model", "Parsed manually - model should continue");
            }
            return new NextSpeakerResponse("user", "Parsed manually - user should speak");
        } catch (Exception e) {
            return new NextSpeakerResponse("user", "Manual parsing failed");
        }
    }

    /**
     * 检查响应内容是否表明需要继续 - 优化版本
     */
    public boolean shouldContinueBasedOnContent(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }

        String lowerResponse = response.toLowerCase();

        // 优先检查明确的停止指示词 - 扩展版本
        String[] stopIndicators = {
            "completed", "finished", "done", "ready", "all set", "task complete",
            "project complete", "successfully created all", "that's it", "we're done",
            "everything is ready", "all files created", "project is ready",
            "task completed successfully", "all tasks completed", "work is complete",
            "implementation complete", "setup complete", "configuration complete",
            "files have been created", "project has been set up", "website is ready",
            "application is ready", "all necessary files", "setup is complete"
        };

        // 检查停止指示词
        for (String indicator : stopIndicators) {
            if (lowerResponse.contains(indicator)) {
                logger.debug("Found stop indicator: '{}' in response", indicator);
                return false;
            }
        }

        // 检查工具调用成功的模式 - 新增：这是文件编辑场景的关键优化
        if (isToolCallSuccessPattern(response)) {
            logger.debug("Detected successful tool call pattern, should continue");
            return true;
        }

        // 扩展的继续指示词
        String[] continueIndicators = {
            "next, i", "now i", "let me", "i'll", "i will", "moving on",
            "proceeding", "continuing", "then i", "after that", "following this",
            "now let's", "let's now", "i need to", "i should", "i'm going to",
            "next step", "continuing with", "moving to", "proceeding to",
            "now creating", "now editing", "now updating", "now modifying",
            "let me create", "let me edit", "let me update", "let me modify",
            "i'll create", "i'll edit", "i'll update", "i'll modify",
            "creating the", "editing the", "updating the", "modifying the"
        };

        // 检查继续指示词
        for (String indicator : continueIndicators) {
            if (lowerResponse.contains(indicator)) {
                logger.debug("Found continue indicator: '{}' in response", indicator);
                return true;
            }
        }

        // 检查是否包含文件操作相关内容 - 针对文件编辑场景优化
        if (containsFileOperationIntent(lowerResponse)) {
            logger.debug("Detected file operation intent, should continue");
            return true;
        }

        // 如果响应很短且没有明确结束，可能需要继续
        boolean shortResponseContinue = response.length() < 200 && !lowerResponse.contains("?");
        if (shortResponseContinue) {
            logger.debug("Short response without question mark, should continue");
        }

        return shortResponseContinue;
    }

    /**
     * 检查是否是工具调用成功的模式
     */
    private boolean isToolCallSuccessPattern(String response) {
        String lowerResponse = response.toLowerCase();

        // 工具调用成功的典型模式
        String[] toolSuccessPatterns = {
            "successfully created",
            "successfully updated",
            "successfully modified",
            "successfully edited",
            "file created",
            "file updated",
            "file modified",
            "file edited",
            "created file",
            "updated file",
            "modified file",
            "edited file",
            "✅", // 成功标记
            "file has been created",
            "file has been updated",
            "file has been modified",
            "content has been",
            "successfully wrote",
            "successfully saved"
        };

        for (String pattern : toolSuccessPatterns) {
            if (lowerResponse.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查是否包含文件操作意图
     */
    private boolean containsFileOperationIntent(String lowerResponse) {
        String[] fileOperationIntents = {
            "create a", "create the", "creating a", "creating the",
            "edit a", "edit the", "editing a", "editing the",
            "update a", "update the", "updating a", "updating the",
            "modify a", "modify the", "modifying a", "modifying the",
            "write a", "write the", "writing a", "writing the",
            "generate a", "generate the", "generating a", "generating the",
            "add to", "adding to", "append to", "appending to",
            "need to create", "need to edit", "need to update", "need to modify",
            "will create", "will edit", "will update", "will modify",
            "going to create", "going to edit", "going to update", "going to modify"
        };

        for (String intent : fileOperationIntents) {
            if (lowerResponse.contains(intent)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 下一步发言者响应
     */
    public static class NextSpeakerResponse {
        @JsonProperty("next_speaker")
        private String nextSpeaker;
        
        @JsonProperty("reasoning")
        private String reasoning;

        public NextSpeakerResponse() {}

        public NextSpeakerResponse(String nextSpeaker, String reasoning) {
            this.nextSpeaker = nextSpeaker;
            this.reasoning = reasoning;
        }

        public String getNextSpeaker() {
            return nextSpeaker;
        }

        public void setNextSpeaker(String nextSpeaker) {
            this.nextSpeaker = nextSpeaker;
        }

        public String getReasoning() {
            return reasoning;
        }

        public void setReasoning(String reasoning) {
            this.reasoning = reasoning;
        }

        public boolean isModelNext() {
            return "model".equals(nextSpeaker);
        }

        public boolean isUserNext() {
            return "user".equals(nextSpeaker);
        }

        @Override
        public String toString() {
            return String.format("NextSpeaker{speaker='%s', reasoning='%s'}", nextSpeaker, reasoning);
        }
    }
}
