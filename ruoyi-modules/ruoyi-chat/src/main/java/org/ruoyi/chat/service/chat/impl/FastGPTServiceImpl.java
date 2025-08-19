package org.ruoyi.chat.service.chat.impl;

import org.ruoyi.chat.config.ChatConfig;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.listener.FastGPTSSEEventSourceListener;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.entity.chat.FastGPTChatCompletion;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * FastGpt 聊天管理
 * 项目整体沿用Openai接口范式，根据FastGPT文档增加相应的参数
 *
 * @author yzm
 */
@Service
public class FastGPTServiceImpl implements IChatService {

    @Autowired
    private IChatModelService chatModelService;

    @Override
    public SseEmitter chat(ChatRequest chatRequest, SseEmitter emitter) {
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        OpenAiStreamClient openAiStreamClient = ChatConfig.createOpenAiStreamClient(chatModelVo.getApiHost(), chatModelVo.getApiKey());
        List<Message> messages = chatRequest.getMessages();
        FastGPTSSEEventSourceListener listener = new FastGPTSSEEventSourceListener(emitter, chatRequest.getSessionId());
        FastGPTChatCompletion completion = FastGPTChatCompletion
                .builder()
                .messages(messages)
                // 开启后sse会返回event值
                .detail(true)
                .stream(true)
                .build();
        try {
            openAiStreamClient.streamChatCompletion(completion, listener);
        } catch (Exception ex) {
            org.ruoyi.chat.support.RetryNotifier.notifyFailure(chatRequest.getSessionId());
            throw ex;
        }
        return emitter;
    }

    @Override
    public String getCategory() {
        return ChatModeType.FASTGPT.getCode();
    }
}
