package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.chat.impl.AbstractStreamingChatService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
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

    // 添加文档解析的前缀字段
    private static final String UPLOAD_FILE_API_PREFIX = "fileid";

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo,ChatRequest chatRequest) {
        return QwenStreamingChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .build();
    }

    @Override
    public void doChat(ChatModelVo chatModelVo,ChatRequest chatRequest,List<ChatMessage> messagesWithMemory,
                          StreamingChatResponseHandler handler) {
        StreamingChatModel streamingChatModel = buildStreamingChatModel(chatModelVo,chatRequest);
        // 判断是否存在需要使用阿里千问的文档解析功能
        List<ChatMessage> chatMessages = hasFileIdData(messagesWithMemory);
        streamingChatModel.chat(chatMessages, handler);
    }

    /**
     * 检查是否包含fileId数据
     */
    private List<ChatMessage> hasFileIdData(List<ChatMessage> messagesWithMemory) {
        if (CollectionUtils.isEmpty(messagesWithMemory)) {
            return messagesWithMemory;
        }

        // 找到包含阿里上传文件前缀的用户信息
        var foundUserMessage = messagesWithMemory.stream()
            .filter(message -> message instanceof UserMessage)
            .map(message -> (UserMessage) message)
            .filter(userMessage ->
                userMessage.singleText().toLowerCase().contains(UPLOAD_FILE_API_PREFIX.toLowerCase())
            )
            .findFirst();

        // 找到原本SystemMessage
        var systemMessage = messagesWithMemory.stream()
            .filter(message -> message instanceof SystemMessage)
            .map(message -> (SystemMessage) message)
            .findFirst();

        // 判断是否存在并重新构建信息体(符合千问文档解析格式)
        return foundUserMessage.map(userMsg -> {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(userMsg.singleText()));
            systemMessage.ifPresent(sysMsg -> messages.add(new UserMessage(sysMsg.text())));
            return messages;
        }).orElse(messagesWithMemory);
    }

    @Override
    public String getProviderName() {
        return ChatModeType.QIAN_WEN.getCode();
    }

}
