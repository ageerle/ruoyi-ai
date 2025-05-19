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
     *  是否开启联网搜索(0关闭 1开启)
     */
    private Boolean search = Boolean.FALSE;

    /**
     *  是否开启mcp
     */
    private Boolean isMcp = Boolean.FALSE;

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
     * 上下文的条数
     */
    private Integer contentNumber = 10;

    /**
     * 是否携带上下文
     */
    private Boolean usingContext = Boolean.TRUE;


}
