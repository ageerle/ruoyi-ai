package tech.ordinaryroad.live.chat.client.douyu.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;

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

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造函数
     *
     * @param role         角色
     * @param content      描述主题信息
     * @param name         name
     */
    public Message(String role, String content, String name) {
        this.role = role;
        this.content = content;
        this.name = name;
    }

    public Message() {
    }

    private Message(Builder builder) {
        setRole(builder.role);
        setContent(builder.content);
        setName(builder.name);
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


        public Message build() {
            return new Message(this);
        }
    }
}
