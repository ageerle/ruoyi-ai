package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.impl.provider.model.DifyStreamingChatModel;
import org.springframework.stereotype.Service;

/**
 * Dify 平台对话服务
 * <p>
 * 通过 dify-java-client 接入 Dify 的对话型应用 (Chat App) 和
 * 工作流编排对话应用 (Chatflow App)，支持流式 SSE 响应。
 *
 * @author better
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DifyChatServiceImpl implements AbstractChatService {

    private final DifyConversationService difyConversationService;

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        return new DifyStreamingChatModel(chatModelVo, chatRequest, difyConversationService);
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        throw new UnsupportedOperationException("Dify 不支持同步 ChatModel，请使用流式模式");
    }

    @Override
    public String getProviderName() {
        return ChatModeType.DIFY.getCode();
    }
}
