package org.ruoyi.common.chat.domain.dto.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import dev.langchain4j.data.message.ChatMessage;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;


/**
 *  对话请求对象
 *
 * @author ageerle
 * @date 2023-04-08
 */
@Data
public class ChatRequest {

    @NotEmpty(message = "传入的模型不能为空")
    private String model;

    /**
     * 对话消息
     */
    @NotEmpty(message = "对话消息不能为空")
    private String content;

    /**
     * 工作流请求体
     */
    private WorkFlowRunner workFlowRunner;

    /**
     * 人机交互信息体
     */
    private ReSumeRunner reSumeRunner;

    /**
     * 是否为人机交互用户继续输入
     */
    private Boolean isResume = false;

    /**
     * 是否启用工作流
     */
    private Boolean enableWorkFlow = false;

    /**
     * 会话id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JSONField(serializeUsing = String.class)
    private Long sessionId;

    /**
     * 知识库id
     */
    private String knowledgeId;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 对话id(每个聊天窗口都不一样)
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JSONField(serializeUsing = String.class)
    private Long uuid;

    /**
     * 是否启用深度思考
     */
    private Boolean enableThinking = false;

    /**
     * 对话模型详情
     */
    private ChatModelVo chatModelVo;

    /**
     * 对话事件
     */
    private SseEmitter emitter;

    /**
     * 当前登录用户id
     */
    private Long userId;

    /**
     * 当前登录用户TOKEN
     */
    private String tokenValue;

    /**
     * 完整的上下文
     */
    private List<ChatMessage> contextMessages;

}
