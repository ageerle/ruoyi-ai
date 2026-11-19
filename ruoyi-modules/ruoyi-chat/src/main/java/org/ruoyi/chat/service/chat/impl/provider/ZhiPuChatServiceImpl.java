package org.ruoyi.chat.service.chat.impl.provider;


import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.observability.MyChatModelListener;
import org.ruoyi.chat.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;


/**
 * 智谱AI服务调用
 *
 * @author zengxb
 * @date 2026/02/26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ZhiPuChatServiceImpl implements AbstractChatService {

    private static final int MAX_TOKENS = 65536;

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        var builder = ZhipuAiStreamingChatModel.builder()
            .apiKey(chatModelVo.getApiKey())
            .model(chatModelVo.getModelName())
            .readTimeout(Duration.ofSeconds(300))
            .listeners(List.of(new MyChatModelListener()));
        if (!chatRequest.isImageInput()) {
            builder.maxToken(MAX_TOKENS);
        }
        return builder.build();
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        return ZhipuAiChatModel.builder()
            .apiKey(chatModelVo.getApiKey())
            .model(chatModelVo.getModelName())
            .maxToken(MAX_TOKENS)
            .readTimeout(Duration.ofSeconds(300))
            .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.ZHI_PU.getCode();
    }
}
