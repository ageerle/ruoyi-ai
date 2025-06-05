package org.ruoyi.chat.service.chat.impl;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingResponseBuilder;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.modelcontextprotocol.client.McpSyncClient;
import io.reactivex.Flowable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.config.ChatConfig;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.listener.SSEEventSourceListener;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

/**
 * 图片识别模型
 */
@Service
@Slf4j
public class ImageQianwenServiceImpl implements IChatService {

    @Autowired
    private IChatModelService chatModelService;

    @SneakyThrows
    @Override
    public SseEmitter chat(ChatRequest chatRequest, SseEmitter emitter) {
        ChatModelVo chatModelVo = chatModelService.selectModelByCategory("image");

        // 发送流式消息

            MultiModalConversation conv = new MultiModalConversation();
            MultiModalMessage systemMessage = MultiModalMessage.builder().role(Role.SYSTEM.getValue())
                    .content(Arrays.asList(
                            Collections.singletonMap("text",chatRequest.getSysPrompt()))).build();
            // 获取用户消息内容
            List<Message> messages = chatRequest.getMessages();
            MultiModalMessage userMessage = null;
            //漫长的格式转换
            // 遍历消息列表，提取文本内容
        if (messages != null && !messages.isEmpty()) {
            Object content = messages.get(messages.size() - 1).getContent();
            List<Map<String, Object>> contentList = new ArrayList<>();
            StringBuilder textContent = new StringBuilder();
            if (content instanceof List<?>) {
                for (Object item : (List<?>) content) {
                    if (item instanceof Map<?, ?> mapItem) {
                        String type = (String) mapItem.get("type");
                        if ("text".equals(type)) {
                            String text = (String) mapItem.get("text");
                            if (text != null) {
                                textContent.append(text).append(" ");
                            }
                        } else if ("image_url".equals(type)) {
                            Map<String, String> imageUrl  = (Map<String, String>) mapItem.get("image_url");
                                contentList.add(Collections.singletonMap("image", imageUrl.get("url")));
                        }
                    }
                }
            }
            // 将拼接后的文本内容添加到 contentList
            if (textContent.length() > 0) {
                contentList.add(Collections.singletonMap("text", textContent.toString().trim()));
            }
            userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(contentList)
                    .build();
        }
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(chatModelVo.getApiKey())
                    .model(chatModelVo.getModelName())
                    .messages(Arrays.asList(systemMessage, userMessage))
                    .incrementalOutput(true)
                    .build();


        try {
            final QwenStreamingResponseBuilder responseBuilder = new QwenStreamingResponseBuilder(param.getModel(),param.getIncrementalOutput() );
            conv.streamCall(param, new ResultCallback<>() {
                @SneakyThrows
                public void onEvent(MultiModalConversationResult result) {

                    String delta = responseBuilder.append(result);
                    if (Utils.isNotNullOrEmpty(delta)) {

                        emitter.send(delta);
                        log.info("收到消息片段: {}", delta);
                    }
                }
                public void onComplete() {
                    emitter.complete();
                    log.info("消息结束", responseBuilder.build());
                }
                public void onError(Exception e) {
                    log.info("请求失败", e.getMessage());
                }
            });
        } catch (NoApiKeyException e) {
            emitter.send("请先配置API密钥");
            throw new RuntimeException(e);
        } catch (UploadFileException e) {
            throw new RuntimeException(e);
        }


        return emitter;
    }
        @Override
    public String getCategory() {
        return ChatModeType.IMAGE.getCode();
    }
}
