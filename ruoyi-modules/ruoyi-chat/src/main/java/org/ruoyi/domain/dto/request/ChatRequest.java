package org.ruoyi.domain.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.ruoyi.domain.dto.ChatMessageDTO;
import java.util.List;

/**
 *  对话请求对象
 *
 * @author ageerle
 * @date 2023-04-08
 */
@Data
public class ChatRequest {

    @NotEmpty(message = "对话消息不能为空")
    private List<ChatMessageDTO> messages;

    @NotEmpty(message = "传入的模型不能为空")
    private String model;

    /**
     * 会话id
     */
    private Long sessionId;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 知识库id
     */
    private String knowledgeId;

    /**
     * 对话id(每个聊天窗口都不一样)
     */
    private Long uuid;

    /**
     * 是否启用深度思考
     */
    private Boolean enableThinking;

    /**
     * 是否自动切换模型
     */
    private Boolean autoSelectModel;

    /**
     * 是否支持联网
     */
    private Boolean enableInternet;

    /**
     * 会话令牌（为避免在非Web线程中获取Request，入口处注入）
     */
    private String token;

}
