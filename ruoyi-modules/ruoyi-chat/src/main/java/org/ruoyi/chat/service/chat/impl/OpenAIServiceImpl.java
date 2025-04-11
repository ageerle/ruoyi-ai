package org.ruoyi.chat.service.chat.impl;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.config.ChatConfig;
import org.ruoyi.chat.listener.SSEEventSourceListener;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
public class OpenAIServiceImpl implements IChatService {

    @Autowired
    private  IChatModelService chatModelService;
    @Autowired
    private ChatConfig chatConfig;
    @Autowired
    private OpenAiStreamClient openAiStreamClient;

    @Override
    public SseEmitter chat(ChatRequest chatRequest,SseEmitter emitter) {

        SSEEventSourceListener openAIEventSourceListener = new SSEEventSourceListener(emitter);
        // 查询模型信息
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());

        if(chatModelVo!=null){
            // 建请求客户端
            openAiStreamClient = chatConfig.createOpenAiStreamClient(chatModelVo.getApiHost(), chatModelVo.getApiKey());
            // 设置默认提示词
            chatRequest.setSysPrompt(chatModelVo.getSystemPrompt());
        }
        ChatCompletion completion = ChatCompletion
                .builder()
                .messages(chatRequest.getMessages())
                .model(chatRequest.getModel())
                .stream(chatRequest.getStream())
                .build();
        openAiStreamClient.streamChatCompletion(completion, openAIEventSourceListener);

        return emitter;
    }
}
