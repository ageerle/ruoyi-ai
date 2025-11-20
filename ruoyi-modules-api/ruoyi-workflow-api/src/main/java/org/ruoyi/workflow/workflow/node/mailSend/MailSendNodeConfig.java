package org.ruoyi.workflow.workflow.node.mailSend;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MailSendNodeConfig {
    private SmtpConfig smtp;
    private SenderConfig sender;
    private String subject;
    private String content;

    @JsonProperty("to_mails")
    private String toMails;   // 支持 "a@x.com,b@y.com"

    @JsonProperty("cc_mails")
    private String ccMails;   // 可选
    private Integer senderType;

    @Data
    public static class SmtpConfig {
        private String host;
        private Integer port; // 465 或 587
    }

    @Data
    public static class SenderConfig {
        private String mail;
        private String name;
        private String password; // 授权码
    }
}
