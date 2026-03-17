package org.ruoyi.common.chat.service.chatMessage;

import org.ruoyi.common.chat.domain.bo.chat.ChatMessageBo;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 聊天信息抽象基类 - 保存聊天信息
 *
 * @author Zengxb
 * @date 2026-02-24
 */
public abstract class AbstractChatMessageService {

    /**
     * 创建日志对象
     */
    Logger log = LoggerFactory.getLogger(AbstractChatMessageService.class);

    @Autowired
    private IChatMessageService chatMessageService;

    /**
     * 保存聊天信息
     */
    public void saveChatMessage(ChatRequest chatRequest, Long userId, String content, String role, ChatModelVo chatModelVo){
        try {
            // 验证必要的上下文信息
            if (chatRequest == null || userId == null) {
                log.warn("缺少必要的聊天上下文信息，无法保存消息");
                return;
            }

            // 创建ChatMessageBo对象
            ChatMessageBo messageBO = new ChatMessageBo();
            messageBO.setUserId(userId);
            messageBO.setSessionId(chatRequest.getSessionId());
            messageBO.setContent(content);
            messageBO.setRole(role);
            messageBO.setModelName(chatRequest.getModel());
            messageBO.setRemark(null);

            chatMessageService.insertByBo(messageBO);
        } catch (Exception e) {
            log.error("保存{}聊天消息时出错: {}", getProviderName(), e.getMessage(), e);
        }
    }

    /**
     * 获取服务提供商名称
     */
    protected String getProviderName(){
        return "默认工作流大模型";
    }
}
