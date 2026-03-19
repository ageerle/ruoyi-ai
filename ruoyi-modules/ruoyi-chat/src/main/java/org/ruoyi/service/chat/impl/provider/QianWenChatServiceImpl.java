package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;


/**
 * qianWenAI服务调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
public class QianWenChatServiceImpl implements AbstractChatService {

    // 添加文档解析的前缀字段
    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo,ChatRequest chatRequest) {
        return QwenStreamingChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.QIAN_WEN.getCode();
    }

}
