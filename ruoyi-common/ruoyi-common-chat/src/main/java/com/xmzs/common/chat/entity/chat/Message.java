package com.xmzs.common.chat.entity.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @since 2023-03-02
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message implements Serializable {

    /**
     * 目前支持四个中角色参考官网，进行情景输入：
     * https://platform.openai.com/docs/guides/chat/introduction
     */
    private String role;

    private String content;

    private String name;

    @JsonProperty("function_call")
    private FunctionCall functionCall;

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造函数
     *
     * @param role         角色
     * @param content      描述主题信息
     * @param name         name
     * @param functionCall functionCall
     */
    public Message(String role, String content, String name, FunctionCall functionCall) {
        this.role = role;
        this.content = content;
        this.name = name;
        this.functionCall = functionCall;
    }

    public Message() {
    }

    private Message(Builder builder) {
        setRole(builder.role);
        setContent(builder.content);
        setName(builder.name);
        setFunctionCall(builder.functionCall);
    }


    @Getter
    @AllArgsConstructor
    public enum Role {

        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant"),
        FUNCTION("function"),
        ;
        private String name;
    }

    public static final class Builder {
        private String role;
        private String content;
        private String name;
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

        public Builder content(String content) {
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

        public Message build() {
            return new Message(this);
        }
    }
}
