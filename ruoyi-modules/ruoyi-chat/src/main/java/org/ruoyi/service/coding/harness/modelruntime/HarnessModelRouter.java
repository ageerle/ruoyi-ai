package org.ruoyi.service.coding.harness.modelruntime;

import org.ruoyi.service.coding.harness.model.HarnessModelRoute;
import org.ruoyi.service.coding.harness.model.HarnessModelRouteSource;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;
import org.ruoyi.service.coding.harness.model.HarnessTaskClass;

/** Pure run-level router: identical inputs always produce an identical durable route. */
public final class HarnessModelRouter {

    public HarnessModelRoute route(String sessionModel, String requirement, boolean hasImages) {
        return route(sessionModel, requirement, hasImages, true);
    }

    /**
     * Routes one run.
     *
     * <p>Doubao-Seed-Evolving 会话固定使用所配置的 Doubao 模型（{@code SESSION_FIXED}），
     * 主循环、恢复、分析、摘要压缩的所有模型调用都保持 Doubao，严禁 fallback 到 DeepSeek。
     * DeepSeek 仍保留原有 deepseek-auto 自动路由行为。</p>
     *
     * @param doubaoThinkingEnabled Doubao 会话思考开关（会话思考等级非 none 时为 true）；
     *                              非 Doubao 模型忽略。
     */
    public HarnessModelRoute route(String sessionModel, String requirement, boolean hasImages,
                                   boolean doubaoThinkingEnabled) {
        String requestedModel = HarnessModelPolicy.normalizeModel(sessionModel);
        if (HarnessModelPolicy.isDoubao(requestedModel)) {
            // Doubao 固定模型：不参与 DeepSeek 自动候选与回退。
            return new HarnessModelRoute(HarnessModelPolicy.POLICY_VERSION, requestedModel,
                requestedModel, HarnessModelPolicy.classify(requirement, hasImages),
                doubaoThinkingEnabled, HarnessModelRouteSource.SESSION_FIXED);
        }
        HarnessTaskClass taskClass = HarnessModelPolicy.classify(requirement, hasImages);
        boolean automatic = HarnessModelPolicy.isAutomatic(requestedModel);
        String selectedModel = automatic
            ? HarnessModelPolicy.selectAutomaticModel(taskClass)
            : requestedModel;
        boolean thinking = HarnessModelPolicy.enableThinking(selectedModel, taskClass);
        return new HarnessModelRoute(HarnessModelPolicy.POLICY_VERSION, requestedModel,
            selectedModel, taskClass, thinking, automatic
                ? HarnessModelRouteSource.AUTO_POLICY : HarnessModelRouteSource.SESSION_FIXED);
    }

    /** Convenience route that derives the Doubao thinking switch from the session. */
    public HarnessModelRoute route(HarnessSessionState session, String requirement,
                                   boolean hasImages) {
        String model = session.model();
        boolean doubaoThinking = !HarnessModelPolicy.isDoubao(model)
            || session.thinkingLevel().thinkingEnabled();
        return route(model, requirement, hasImages, doubaoThinking);
    }

    /** Deterministic compatibility route for snapshots written before run-level routing existed. */
    public HarnessModelRoute legacySessionFallback(String sessionModel, String requirement) {
        HarnessModelRoute resolved = route(sessionModel, requirement, false);
        return new HarnessModelRoute(resolved.policyVersion(), resolved.requestedModel(),
            resolved.selectedModel(), resolved.taskClass(), resolved.thinkingEnabled(),
            HarnessModelRouteSource.LEGACY_SESSION_FALLBACK);
    }
}
