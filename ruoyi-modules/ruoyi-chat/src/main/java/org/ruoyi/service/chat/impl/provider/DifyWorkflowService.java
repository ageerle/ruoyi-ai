package org.ruoyi.service.chat.impl.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.imfangs.dify.client.DifyClientFactory;
import io.github.imfangs.dify.client.DifyWorkflowClient;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.ErrorEvent;
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent;
import io.github.imfangs.dify.client.event.WorkflowTextChunkEvent;
import io.github.imfangs.dify.client.callback.WorkflowStreamCallback;
import io.github.imfangs.dify.client.model.workflow.WorkflowRunRequest;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.dto.request.WorkFlowRunner;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Dify 工作流执行服务
 * <p>
 * 通过 DifyWorkflowClient 调用 Dify 平台上部署的工作流应用，
 * 并将节点事件通过 SSE 实时推送给前端。
 *
 * @author better
 */
@Service
@Slf4j
public class DifyWorkflowService {

    /**
     * 流式执行 Dify 工作流
     *
     * @param chatModelVo 模型配置（apiHost= Dify 地址, apiKey= Dify 密钥）
     * @param chatRequest 聊天请求
     * @return SSE emitter
     */
    public SseEmitter streaming(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        Long userId = chatRequest.getUserId();
        String tokenValue = chatRequest.getTokenValue();
        SseEmitter emitter = chatRequest.getEmitter();

        // 构建 Dify 工作流请求参数
        Map<String, Object> inputs = convertInputs(chatRequest.getWorkFlowRunner());

        WorkflowRunRequest request = WorkflowRunRequest.builder()
                .inputs(inputs)
                .responseMode(ResponseMode.STREAMING)
                .user(String.valueOf(userId))
                .build();

        DifyWorkflowClient client = DifyClientFactory.createWorkflowClient(
                normalizeBaseUrl(chatModelVo.getApiHost()),
                chatModelVo.getApiKey());

        // 异步执行，避免阻塞请求线程
        CompletableFuture.runAsync(() -> {
            try {
                client.runWorkflowStream(request, new WorkflowStreamCallback() {

                    @Override
                    public void onWorkflowTextChunk(WorkflowTextChunkEvent event) {
                        String text = event.getData() != null ? event.getData().getText() : null;
                        if (text != null) {
                            SseMessageUtils.sendContent(userId, text);
                        }
                    }

                    @Override
                    public void onWorkflowFinished(WorkflowFinishedEvent event) {
                        // 将最终输出作为内容发送
                        if (event.getData() != null && event.getData().getOutputs() != null) {
                            Map<String, Object> outputs = event.getData().getOutputs();
                            for (Map.Entry<String, Object> entry : outputs.entrySet()) {
                                SseMessageUtils.sendContent(userId,
                                        entry.getKey() + ": " + entry.getValue() + "\n");
                            }
                        }
                        SseMessageUtils.sendDone(userId);
                        SseMessageUtils.completeConnection(userId, tokenValue);
                    }

                    @Override
                    public void onError(ErrorEvent event) {
                        SseMessageUtils.sendError(userId, event.getMessage());
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        log.error("Dify 工作流执行异常", throwable);
                        SseMessageUtils.sendError(userId, throwable.getMessage());
                        SseMessageUtils.completeConnection(userId, tokenValue);
                    }
                });
            } catch (Exception e) {
                log.error("Dify 工作流执行失败", e);
                SseMessageUtils.sendError(userId, e.getMessage());
                SseMessageUtils.completeConnection(userId, tokenValue);
            }
        });

        return emitter;
    }

    /**
     * 将 WorkFlowRunner.inputs (List<ObjectNode>) 转换为 Dify 所需的 Map
     */
    private Map<String, Object> convertInputs(WorkFlowRunner runner) {
        Map<String, Object> result = new HashMap<>();
        if (runner == null || runner.getInputs() == null) {
            return result;
        }
        for (ObjectNode node : runner.getInputs()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                result.put(field.getKey(), field.getValue().asText());
            }
        }
        return result;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Dify API 地址(apiHost)不能为空");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
