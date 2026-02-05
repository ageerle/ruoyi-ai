package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.domain.dto.request.ChatRequest;
import org.ruoyi.domain.vo.chat.ChatModelVo;
import org.ruoyi.service.chat.impl.AbstractStreamingChatService;
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
public class QianWenChatServiceImpl extends AbstractStreamingChatService {

    @Override
    protected StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo,ChatRequest chatRequest) {
        return QwenStreamingChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .build();
    }

    @Override
    protected void doChat(ChatModelVo chatModelVo,ChatRequest chatRequest,List<ChatMessage> messagesWithMemory,
                          StreamingChatResponseHandler handler) {
        StreamingChatModel streamingChatModel = buildStreamingChatModel(chatModelVo,chatRequest);
        streamingChatModel.chat(messagesWithMemory, handler);
    }

    @Override
    public String getProviderName() {
        return ChatModeType.QIAN_WEN.getCode();
    }

}
