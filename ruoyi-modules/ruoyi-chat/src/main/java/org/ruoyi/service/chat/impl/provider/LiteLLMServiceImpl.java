package org.ruoyi.service.chat.impl.provider;


import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;


/**
 * LiteLLM chat service.
 * <p>
 * LiteLLM proxy exposes an OpenAI-compatible endpoint, so it reuses the OpenAI
 * protocol builder. Point apiHost at the LiteLLM proxy (e.g. http://localhost:4000/v1)
 * to reach 100+ providers (OpenAI, Anthropic, Gemini, Bedrock, Vertex AI, Azure ...)
 * through a single provider entry with unified auth, routing and observability.
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LiteLLMServiceImpl implements AbstractChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(chatModelVo.getApiHost())
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .timeout(Duration.ofMinutes(30))
                .listeners(List.of(new MyChatModelListener()))
                .returnThinking(chatRequest.getEnableThinking())
                .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.LITE_LLM.getCode();
    }

}
