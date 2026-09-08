package org.ruoyi.service.coding.harness.modelruntime;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.coding.harness.loop.model.ModelTurnException;
import org.ruoyi.service.coding.harness.loop.model.ModelTurnListener;
import org.ruoyi.service.coding.harness.loop.model.StreamingModelTurnAdapter;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/** Bounded advisory worker used for parallel, disjoint analysis tasks. It has no workspace tools. */
@Service
public class HarnessAnalysisDelegate {

    private static final String SYSTEM_PROMPT = """
        You are one bounded coding-analysis worker. Analyze only the assigned question and supplied
        evidence. You have no repository or mutation tools, so never claim to have inspected or
        changed anything outside that evidence. Return a concise result with: conclusion, exact
        evidence, risks/unknowns, and one recommended parent action. Do not create another plan,
        delegate work, or repeat the parent task.
        """;

    private final HarnessModelRegistry modelRegistry;
    private final ChatServiceFactory chatServiceFactory;
    private final ScheduledExecutorService timeoutScheduler;

    public HarnessAnalysisDelegate(
        HarnessModelRegistry modelRegistry,
        ChatServiceFactory chatServiceFactory,
        @Qualifier("codingHarnessModelTimeoutScheduler") ScheduledExecutorService timeoutScheduler
    ) {
        this.modelRegistry = modelRegistry;
        this.chatServiceFactory = chatServiceFactory;
        this.timeoutScheduler = timeoutScheduler;
    }

    public String execute(HarnessSessionState session, HarnessRunState run, String role,
                          String task, String evidence) {
        String boundedRole = requireBounded(role, "role", 80);
        String boundedTask = requireBounded(task, "task", 4_000);
        String boundedEvidence = requireBounded(evidence, "evidence", 60_000);
        String fallbackModel = run.modelRoute() == null
            ? session.model() : run.modelRoute().selectedModel();
        boolean doubaoSession = HarnessModelPolicy.isDoubao(fallbackModel);
        // Doubao 会话的并行分析委托也必须保持 Doubao，严禁 fallback 到 DeepSeek Flash。
        ChatModelVo model = doubaoSession
            ? modelRegistry.requireConfigured(fallbackModel)
            : configuredDelegateModel(fallbackModel);
        AbstractChatService provider = chatServiceFactory.getOriginalService(model.getProviderCode());
        org.ruoyi.common.chat.domain.dto.request.ChatRequest providerRequest =
            new org.ruoyi.common.chat.domain.dto.request.ChatRequest();
        providerRequest.setModel(model.getModelName());
        providerRequest.setContent(boundedTask);
        providerRequest.setEnableThinking(false);
        providerRequest.setUserId(run.userId());
        providerRequest.setChatModelVo(model);
        if (doubaoSession) {
            // 有界分析轮不使用长思考：固定 Doubao，思考等级保持会话配置（默认 high）。
            org.ruoyi.service.coding.harness.model.HarnessThinkingLevel level =
                session.thinkingLevel();
            providerRequest.setEnableThinking(level.thinkingEnabled());
            providerRequest.setReasoningEffort(level.wireValue());
        }
        StreamingChatModel streamingModel = provider.buildStreamingChatModel(model, providerRequest);
        dev.langchain4j.model.chat.request.ChatRequest request =
            dev.langchain4j.model.chat.request.ChatRequest.builder()
                .messages(List.of(SystemMessage.from(SYSTEM_PROMPT), UserMessage.from(
                    "Worker role: " + boundedRole + "\n\nAssigned question:\n" + boundedTask
                        + "\n\nParent-supplied evidence:\n" + boundedEvidence)))
                .parameters(ChatRequestParameters.builder().maxOutputTokens(1_600).build())
                .build();
        try {
            var turn = new StreamingModelTurnAdapter(streamingModel, timeoutScheduler,
                Clock.systemUTC()).execute(request, Duration.ofSeconds(90), ModelTurnListener.NOOP);
            String text = turn.response().aiMessage().text();
            return text == null || text.isBlank() ? "Worker returned no analysis" : text;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Delegated analysis was interrupted", interrupted);
        } catch (ModelTurnException failure) {
            throw new IllegalStateException("Delegated analysis failed: " + failure.getMessage(), failure);
        }
    }

    private ChatModelVo configuredDelegateModel(String fallback) {
        try {
            return modelRegistry.requireConfigured(HarnessModelPolicy.FLASH);
        } catch (RuntimeException unavailable) {
            return modelRegistry.requireConfigured(fallback);
        }
    }

    private String requireBounded(String value, String name, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new IllegalArgumentException(name + " is required and must not exceed "
                + maximum + " characters");
        }
        return value.strip();
    }
}
