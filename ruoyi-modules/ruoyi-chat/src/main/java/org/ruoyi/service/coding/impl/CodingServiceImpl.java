package org.ruoyi.service.coding.impl;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.domain.bo.coding.CodingRequestBo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.mcp.tools.DeleteFileTool;
import org.ruoyi.mcp.tools.EditFileTool;
import org.ruoyi.mcp.tools.ExecuteCommandTool;
import org.ruoyi.mcp.tools.ListDirectoryTool;
import org.ruoyi.mcp.tools.ReadFileTool;
import org.ruoyi.mcp.tools.WriteFileTool;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.coding.CodingAgent;
import org.ruoyi.service.coding.CodingEventChannel;
import org.ruoyi.service.coding.CodingSseEvent;
import org.ruoyi.service.coding.CodingWorkspaceService;
import org.ruoyi.service.coding.ICodingService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 编程能力 Service 实现。
 *
 * <p>B 路径：自建 SseEmitter（不进 SseEmitterManager 全局注册表），照 ShortDramaServiceImpl 骨架。
 * 拿模型三步（skill 铁律）→ 解析工作目录 → new 工具实例注入 channel+root → AiServices 构建 →
 * 异步执行，工具内部通过 channel 实时推事件，drain 线程把事件写到 emitter。
 *
 * @author ageerle
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodingServiceImpl implements ICodingService {

    static final String SAFE_CODING_ERROR_MESSAGE = "编程任务执行失败，请稍后重试";

    private final IChatModelService chatModelService;
    private final ChatServiceFactory chatServiceFactory;
    private final CodingWorkspaceService workspaceService;
    private final Map<SseEmitter, AtomicBoolean> activeEmitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter chat(CodingRequestBo bo, Long userId) {
        // 在建立 SSE 和调用模型前同步拒绝非受控工作区，让调用方获得明确错误。
        Path root = workspaceService.resolveRoot(bo.getWorkspacePath());

        SseEmitter emitter = new SseEmitter(1_800_000L);
        AtomicBoolean emitterActive = new AtomicBoolean(true);
        activeEmitters.put(emitter, emitterActive);
        emitter.onCompletion(() -> closeEmitter(emitter));
        emitter.onTimeout(() -> closeEmitter(emitter));
        emitter.onError(error -> closeEmitter(emitter));

        CompletableFuture.runAsync(() -> {
            CodingEventChannel channel = new CodingEventChannel();
            Thread drainThread = new Thread(() -> {
                try {
                    channel.drain(event -> sendEmitterEvent(emitter, toSseEvent(event)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    log.error("coding_sse operation=DRAIN status=FAILED errorType={}", errorType(t));
                }
            }, "coding-sse-drain");
            drainThread.start();

            try {
                // 推送思考开始
                channel.send(CodingSseEvent.thinking("正在分析指令..."));

                // 1. 拿模型三步（不硬编码配置）
                ChatModelVo modelVo = chatModelService.selectModelByName(bo.getModel());
                if (modelVo == null) {
                    throw new IllegalStateException("模型未找到: " + bo.getModel()
                        + "，请在 chat_model 表配置该模型名称");
                }
                AbstractChatService chatService = chatServiceFactory.getOriginalService(modelVo.getProviderCode());
                ChatModel chatModel = chatService.buildChatModel(modelVo);

                // 2. 解析工作目录
                Files.createDirectories(root);

                // 3. new 工具实例（不走 BuiltinToolRegistry，注入会话工作目录与 channel）
                ReadFileTool read = new ReadFileTool(root, channel);
                EditFileTool edit = new EditFileTool(root, channel);
                ListDirectoryTool list = new ListDirectoryTool(root, channel);
                WriteFileTool write = new WriteFileTool(root, channel);
                DeleteFileTool delete = new DeleteFileTool(root, channel);
                ExecuteCommandTool exec = new ExecuteCommandTool(root, channel);

                // 4. 构建 AiServices
                CodingAgent agent = AiServices.builder(CodingAgent.class)
                    .chatModel(chatModel)
                    .tools(read, edit, list, write, delete, exec)
                    .build();

                // 5. 同步调用（方案 B）：工具执行过程中事件通过 channel 实时推送
                String result = agent.chat(bo.getPrompt());

                // 6. 推送最终文本
                if (StrUtil.isNotBlank(result)) {
                    channel.send(CodingSseEvent.text(result));
                }
                channel.send(CodingSseEvent.done());
                channel.complete();
                drainThread.join(5_000);
                completeEmitter(emitter);

            } catch (Exception e) {
                log.error("coding_chat status=FAILED errorType={}", errorType(e));
                channel.send(safeFailureEvent(e));
                channel.complete();
                try {
                    drainThread.join(2_000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                completeEmitter(emitter);
            }
        });

        return emitter;
    }

    /**
     * 把结构化事件转成 SseEmitter 事件。
     */
    private SseEmitter.SseEventBuilder toSseEvent(CodingSseEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (event.filePath() != null) payload.put("filePath", event.filePath());
        if (event.command() != null) payload.put("command", event.command());
        if (event.content() != null) payload.put("content", event.content());
        if (event.status() != null) payload.put("status", event.status());
        return SseEmitter.event()
            .name(event.eventType())
            .data(JsonUtils.toJsonString(payload));
    }

    // ==================== SSE 发送封装（抄自 ShortDramaServiceImpl） ====================

    private boolean sendEmitterEvent(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        AtomicBoolean active = activeEmitters.get(emitter);
        if (active == null || !active.get()) return false;
        try {
            emitter.send(event);
            return true;
        } catch (IOException | IllegalStateException e) {
            closeEmitter(emitter);
            return false;
        }
    }

    private void closeEmitter(SseEmitter emitter) {
        AtomicBoolean active = activeEmitters.remove(emitter);
        if (active != null) active.set(false);
    }

    private void completeEmitter(SseEmitter emitter) {
        AtomicBoolean active = activeEmitters.get(emitter);
        if (active == null || !active.compareAndSet(true, false)) return;
        activeEmitters.remove(emitter);
        try { emitter.complete(); } catch (IllegalStateException ignored) { }
    }

    static CodingSseEvent safeFailureEvent(Throwable ignored) {
        return CodingSseEvent.error(SAFE_CODING_ERROR_MESSAGE);
    }

    private static String errorType(Throwable error) {
        return error == null ? "unknown" : error.getClass().getName();
    }
}
