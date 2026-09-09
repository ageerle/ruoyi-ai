package org.ruoyi.service.coding.harness.modelruntime;

import dev.langchain4j.model.chat.StreamingChatModel;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.coding.harness.model.HarnessModelRoute;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;
import org.ruoyi.service.coding.harness.model.HarnessThinkingLevel;
import org.springframework.stereotype.Service;

@Service
public class RuoYiHarnessChatModelFactory implements HarnessChatModelFactory {

    private final HarnessModelRegistry modelRegistry;
    private final ChatServiceFactory chatServiceFactory;

    public RuoYiHarnessChatModelFactory(HarnessModelRegistry modelRegistry,
                                        ChatServiceFactory chatServiceFactory) {
        this.modelRegistry = modelRegistry;
        this.chatServiceFactory = chatServiceFactory;
    }

    @Override
    public StreamingChatModel create(HarnessSessionState session, HarnessRunState run) {
        HarnessModelRoute route = run.modelRoute();
        String selectedModel = route == null ? session.model() : route.selectedModel();
        // Doubao 会话固定 Doubao：主循环、恢复、分析、摘要压缩的所有模型调用都保持 Doubao，
        // 严禁偷偷 fallback 到 DeepSeek。
        ChatModelVo model = modelRegistry.requireConfigured(selectedModel);
        boolean thinkingEnabled = route == null
            ? HarnessModelPolicy.enableThinking(selectedModel, run.originalRequirement())
            : route.thinkingEnabled();
        return createModel(run, session, model, thinkingEnabled, false);
    }

    @Override
    public ActionModel createRequiredActionModel(HarnessSessionState session,
                                                 HarnessRunState run) {
        HarnessModelRoute route = run.modelRoute();
        String sessionModel = route == null ? session.model() : route.selectedModel();
        ChatModelVo model;
        if (HarnessModelPolicy.isDoubao(sessionModel)) {
            // Doubao 会话动作轮也必须保持 Doubao，不回退 DeepSeek Flash。
            model = modelRegistry.requireConfigured(sessionModel);
        } else {
            try {
                model = modelRegistry.requireConfigured(HarnessModelPolicy.FLASH);
            } catch (RuntimeException unavailable) {
                model = modelRegistry.requireConfigured(sessionModel);
            }
        }
        boolean doubaoSession = HarnessModelPolicy.isDoubao(sessionModel);
        // 当前配置接入的 Doubao 接口实测不接受 tool_choice（required/显式 auto 均 HTTP 400），
        // 动作轮必须声明不支持 forced tool choice；模型选择与思考等级仍固定为 Doubao 会话配置，
        // 严禁通过换模型或关闭思考绕过。非 Doubao 路由保持原有能力声明。
        boolean requiredToolChoiceSupported = !doubaoSession;
        boolean thinkingEnabled = doubaoSession
            && session.thinkingLevel().thinkingEnabled();
        // 基线行为：非 Doubao（DeepSeek 等）动作轮 replayThinking=true，让提供方在动作轮
        // 回放思考；Doubao 的思考加密原文由适配器始终经 encrypted_content 原样回放，不使用该标志。
        boolean replayThinking = !doubaoSession;
        return new ActionModel(createModel(run, session, model, thinkingEnabled, replayThinking),
            thinkingEnabled, requiredToolChoiceSupported);
    }

    @Override
    public ActionModel createEscalatedActionModel(HarnessSessionState session,
                                                  HarnessRunState run) {
        HarnessModelRoute route = run.modelRoute();
        String selectedModel = route == null ? session.model() : route.selectedModel();
        ChatModelVo model = modelRegistry.requireConfigured(selectedModel);
        boolean doubaoModel = HarnessModelPolicy.isDoubao(selectedModel);
        // 与 createRequiredActionModel 相同的 Doubao 实测约束：升级动作轮也不得声称
        // 支持 forced tool choice；保持原会话模型与思考等级不变。
        boolean thinkingEnabled = doubaoModel
            && session.thinkingLevel().thinkingEnabled();
        // 与 createRequiredActionModel 一致：非 Doubao 升级动作轮保持基线 replayThinking=true。
        boolean replayThinking = !doubaoModel;
        return new ActionModel(createModel(run, session, model, thinkingEnabled, replayThinking),
            thinkingEnabled, !doubaoModel);
    }

    private StreamingChatModel createModel(HarnessRunState run, HarnessSessionState session,
                                           ChatModelVo model,
                                           boolean thinkingEnabled,
                                           boolean replayThinking) {
        AbstractChatService provider = chatServiceFactory.getOriginalService(model.getProviderCode());
        ChatRequest request = new ChatRequest();
        request.setModel(model.getModelName());
        request.setContent(run.originalRequirement());
        request.setEnableThinking(thinkingEnabled);
        request.setReplayThinking(replayThinking);
        request.setUserId(run.userId());
        request.setChatModelVo(model);
        if (HarnessModelPolicy.isDoubao(model.getModelName())) {
            // Doubao 思考等级随会话固定；none 关闭思考，其余档位发送对应 reasoning_effort。
            HarnessThinkingLevel level = session == null ? HarnessThinkingLevel.DOUBAO_DEFAULT
                : session.thinkingLevel();
            request.setReasoningEffort(level.wireValue());
            if (level == HarnessThinkingLevel.NONE) {
                request.setEnableThinking(false);
            }
        }
        return provider.buildStreamingChatModel(model, request);
    }
}
