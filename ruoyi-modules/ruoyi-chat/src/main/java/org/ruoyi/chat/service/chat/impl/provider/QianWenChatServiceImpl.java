package org.ruoyi.chat.service.chat.impl.provider;


import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.observability.ChatModelListenerProvider;
import org.ruoyi.chat.observability.MyChatModelListener;
import org.ruoyi.chat.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * qianWenAI服务调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QianWenChatServiceImpl implements AbstractChatService {

    private final ChatModelListenerProvider listenerProvider;

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo,ChatRequest chatRequest) {
        var builder = QwenStreamingChatModel.builder()
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .listeners(List.of(new MyChatModelListener()));
        if (chatRequest.isImageInput()) {
            builder.isMultimodalModel(true);
        }
        return builder.build();
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        return QwenChatModel.builder()
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.QIAN_WEN.getCode();
    }

}
