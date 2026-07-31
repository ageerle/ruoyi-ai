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
import org.ruoyi.service.coding.ICodingService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    /** 默认工作目录：直接指向 ruoyi-copilot 前端项目 */
    private static final String DEFAULT_WORKSPACE = "D:/Project/github/ruoyi-copilot";

    private final IChatModelService chatModelService;
    private final ChatServiceFactory chatServiceFactory;
    private final Map<SseEmitter, AtomicBoolean> activeEmitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter chat(CodingRequestBo bo, Long userId) {
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
                    log.error("编程 SSE drain 线程异常", t);
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
                Path root = resolveWorkspace(bo.getWorkspacePath());
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
                log.error("编程对话失败", e);
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                channel.send(CodingSseEvent.error(msg));
                channel.completeWithError(e);
                try {
                    drainThread.join(2_000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                sendEmitterEvent(emitter, SseEmitter.event().name("error")
                    .data(JsonUtils.toJsonString(Map.of("message", msg))));
                completeEmitterWithError(emitter, e);
            }
        });

        return emitter;
    }

    /**
     * 解析工作目录：前端显式传则用前端的，否则默认 ruoyi-copilot。
     */
    private Path resolveWorkspace(String workspacePath) {
        if (StrUtil.isNotBlank(workspacePath)) {
            return Paths.get(workspacePath).toAbsolutePath().normalize();
        }
        return Paths.get(DEFAULT_WORKSPACE).toAbsolutePath().normalize();
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

    private void completeEmitterWithError(SseEmitter emitter, Throwable error) {
        AtomicBoolean active = activeEmitters.get(emitter);
        if (active == null || !active.compareAndSet(true, false)) return;
        activeEmitters.remove(emitter);
        try { emitter.completeWithError(error); } catch (IllegalStateException ignored) { }
    }
}
