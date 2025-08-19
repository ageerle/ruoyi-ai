package org.ruoyi.common.chat.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.ruoyi.common.chat.entity.chat.Message;

import java.util.List;

/**
 *  对话请求对象
 *
 * @author ageerle
 * @sine 2023-04-08
 */
@Data
public class ChatRequest {

    @NotEmpty(message = "对话消息不能为空")
    List<Message> messages;

    @NotEmpty(message = "传入的模型不能为空")
    private String model;

    /**
     * 提示词
     */
    private String prompt;

    /**
     * 系统提示词
     */
    private String sysPrompt;

    /**
     * 是否开启流式对话
     */
    private Boolean stream = Boolean.TRUE;

    /**
     * 知识库id
     */
    private String kid;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 会话id
     */
    private Long sessionId;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 对话角色
     */
    private String role;


    /**
     * 对话id(每个聊天窗口都不一样)
     */
    private Long uuid;

    /**
     * 是否有附件
     */
    private Boolean hasAttachment;

    /**
     * 是否自动切换模型
     */
    private Boolean autoSelectModel;

}
