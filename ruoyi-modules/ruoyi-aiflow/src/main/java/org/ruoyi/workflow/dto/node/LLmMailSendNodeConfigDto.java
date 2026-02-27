package org.ruoyi.workflow.dto.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 为大模型返回信息封装的信息DTO（发送邮箱）
 */
@Data
public class LLmMailSendNodeConfigDto {

    /**
     * 主题
     */
    private String subject;

    /**
     * 内容
     */
    private String content;

    /**
     * 收件邮箱
     */
    @JsonProperty("to_mails")
    private String toMails;

    /**
     * 抄送邮箱
     */
    @JsonProperty("cc_mails")
    private String ccMails;

    /**
     * 发送类型
     */
    private Integer senderType;
}
