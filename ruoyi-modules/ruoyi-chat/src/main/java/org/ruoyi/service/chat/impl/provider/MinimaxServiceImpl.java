package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MiniMax chat service.
 * <p>
 * Uses MiniMax's OpenAI-compatible API with MiniMax-M3 and MiniMax-M2.7.
 * Global API: https://api.minimax.io/v1
 * China API: https://api.minimaxi.com/v1
 *
 * @author octopus
 * @date 2026/3/21
 */
@Service
@Slf4j
public class MinimaxServiceImpl implements AbstractChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .listeners(List.of(new MyChatModelListener()))
            .returnThinking(chatRequest.getEnableThinking())
            .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.MINIMAX.getCode();
    }

}
