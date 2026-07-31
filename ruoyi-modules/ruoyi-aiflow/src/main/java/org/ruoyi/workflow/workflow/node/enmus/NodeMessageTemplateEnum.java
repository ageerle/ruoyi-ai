package org.ruoyi.workflow.workflow.node.enmus;

import lombok.Getter;

/**
 * 节点消息模板ConfigKey枚举 <br/>
 * 模板优先从 sys_config 读取(可在系统管理-配置管理中自定义), 未配置时回退到 defaultTemplate 内置默认值
 */
@Getter
public enum NodeMessageTemplateEnum {
    HTTP_REQUEST("node.httpRequest.template", "✅ HTTP请求节点：结束响应 - "),
    MAIL_SEND("node.mailsend.template", "📧 发送邮箱节点：结束响应 - "),
    IMAGE("node.image.template", "🎨 文生图节点：结束响应 - 图片URL: "),
    SWITCH("node.switch.template", "🔀 条件分支节点：触发 -> 跳转到节点 "),
    LLM_RESPONSE("node.llmAnswer.template", "🤖 LLM 节点 生成回答："),
    GOOGLE_SEARCH("node.googleSearch.template", "🔍 网络搜索节点处理完成："),
    EXCEPTION("node.exception.template", "🛑 工作流发生异常："),
    END("node.end.template", "🔚 流程已执行完毕，如果您有其他需求，请随时重新发起请求。");

    private final String value;

    /**
     * 内置默认模板, sys_config 未配置对应键时使用
     */
    private final String defaultTemplate;

    NodeMessageTemplateEnum(String value, String defaultTemplate) {
        this.value = value;
        this.defaultTemplate = defaultTemplate;
    }

    /**
     * 根据 configKey 获取内置默认模板, 未知键返回空串
     *
     * @param configKey sys_config 配置键
     * @return 内置默认模板
     */
    public static String getDefaultTemplate(String configKey) {
        for (NodeMessageTemplateEnum item : values()) {
            if (item.value.equals(configKey)) {
                return item.defaultTemplate;
            }
        }
        return "";
    }
}
