package org.ruoyi.workflow.workflow.node.enmus;

import lombok.Getter;

/**
 * 节点消息模板ConfigKey枚举
 */
@Getter
public enum NodeMessageTemplateEnum {
    HTTP_REQUEST("node.httpRequest.template"),
    MAIL_SEND("node.mailsend.template"),
    IMAGE("node.image.template"),
    HUMAN_FEED_BACK("node.humanFeedback.template"),
    SWITCH("node.switch.template"),
    LLM_RESPONSE("node.llmAnswer.template"),
    KEYWORD_EXTRACTOR("node.keywordExtractor.template"),
    EXCEPTION("node.exception.template"),
    END("node.end.template");

    private final String value;

    NodeMessageTemplateEnum(String value) {
        this.value = value;
    }
}
