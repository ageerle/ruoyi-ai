package org.ruoyi.chat.support;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.util.SSEUtil;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * 统一的聊天重试与降级调度器。
 *
 * 策略：
 * - 当前模型最多重试 3 次；仍失败则降级到同分类内、优先级小于当前的最高优先级模型。
 * - 降级模型同样最多重试 3 次；仍失败则向前端返回失败信息并停止。
 *
 * 注意：实现依赖调用方在底层异步失败时执行 onFailure.run() 通知本调度器。
 */
@Slf4j
public class ChatRetryHelper {

    public interface AttemptStarter {
        void start(ChatModelVo model, Runnable onFailure) throws Exception;
    }

    public static void executeWithRetry(
            ChatModelVo primaryModel,
            String category,
            IChatModelService chatModelService,
            SseEmitter emitter,
            AttemptStarter attemptStarter
    ) {
        Objects.requireNonNull(primaryModel, "primaryModel must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(chatModelService, "chatModelService must not be null");
        Objects.requireNonNull(emitter, "emitter must not be null");
        Objects.requireNonNull(attemptStarter, "attemptStarter must not be null");

        AtomicInteger mainAttempts = new AtomicInteger(0);
        AtomicInteger fallbackAttempts = new AtomicInteger(0);
        AtomicBoolean inFallback = new AtomicBoolean(false);
        AtomicBoolean scheduling = new AtomicBoolean(false);

        class Scheduler {
            volatile ChatModelVo current = primaryModel;
            volatile ChatModelVo fallback = null;

            void startAttempt() {
                try {
                    if (!inFallback.get()) {
                        if (mainAttempts.incrementAndGet() > 3) {
                            // 进入降级
                            inFallback.set(true);
                            if (fallback == null) {
                                Integer curPriority = primaryModel.getPriority();
                                if (curPriority == null) {
                                    curPriority = Integer.MAX_VALUE;
                                }
                                fallback = chatModelService.selectFallbackModelByCategoryAndLessPriority(category, curPriority);
                            }
                            if (fallback == null) {
                                SSEUtil.sendErrorEvent(emitter, "当前模型重试3次均失败，且无可用降级模型");
                                emitter.complete();
                                return;
                            }
                            current = fallback;
                            mainAttempts.set(3); // 锁定
                            fallbackAttempts.set(0);
                        }
                    } else {
                        if (fallbackAttempts.incrementAndGet() > 3) {
                            SSEUtil.sendErrorEvent(emitter, "降级模型重试3次仍失败");
                            emitter.complete();
                            return;
                        }
                    }

                    Runnable onFailure = () -> {
                        // 去抖：避免同一次失败触发多次重试
                        if (scheduling.compareAndSet(false, true)) {
                            try {
                                SSEUtil.sendErrorEvent(emitter, (inFallback.get() ? "降级模型" : "当前模型") + "调用失败，准备重试...");
                                // 立即发起下一次尝试
                                startAttempt();
                            } finally {
                                scheduling.set(false);
                            }
                        }
                    };

                    attemptStarter.start(current, onFailure);
                } catch (Exception ex) {
                    log.error("启动聊天尝试失败: {}", ex.getMessage(), ex);
                    SSEUtil.sendErrorEvent(emitter, "启动聊天尝试失败: " + ex.getMessage());
                    // 直接按失败处理，继续重试/降级
                    if (scheduling.compareAndSet(false, true)) {
                        try {
                            startAttempt();
                        } finally {
                            scheduling.set(false);
                        }
                    }
                }
            }
        }

        new Scheduler().startAttempt();
    }
}


