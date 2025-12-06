package org.ruoyi.common.chat.entity.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.ruoyi.common.chat.entity.chat.tool.ToolCalls;

import java.io.Serializable;
import java.util.List;

/**
 * @author https:www.unfbx.com
 * @since 2023-03-02
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class MessagePicture extends BaseMessage implements Serializable {
    /**
     * Content数组支持多图片输入
     * https://platform.openai.com/docs/guides/vision
     */
    private List<Content> content;


    /**
     * 构造函数
     *
     * @param role         角色
     * @param name         name
     * @param content      content
     * @param functionCall functionCall
     */
    public MessagePicture(String role, String name, List<Content> content, List<ToolCalls> toolCalls, String toolCallId, FunctionCall functionCall) {
        this.content = content;
        super.setRole(role);
        super.setName(name);
        super.setToolCalls(toolCalls);
        super.setToolCallId(toolCallId);
        super.setFunctionCall(functionCall);
    }

    public MessagePicture() {
    }

    private MessagePicture(Builder builder) {
        setContent(builder.content);
        super.setRole(builder.role);
        super.setName(builder.name);
        super.setFunctionCall(builder.functionCall);
        super.setToolCalls(builder.toolCalls);
        super.setToolCallId(builder.toolCallId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String role;
        private List<Content> content;
        private String name;
        private String toolCallId;
        private List<ToolCalls> toolCalls;
        private FunctionCall functionCall;

        public Builder() {
        }

        public Builder role(Role role) {
            this.role = role.getName();
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder content(List<Content> content) {
            this.content = content;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder functionCall(FunctionCall functionCall) {
            this.functionCall = functionCall;
            return this;
        }

        public Builder toolCalls(List<ToolCalls> toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }

        public Builder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

        public MessagePicture build() {
            return new MessagePicture(this);
        }
    }

}
