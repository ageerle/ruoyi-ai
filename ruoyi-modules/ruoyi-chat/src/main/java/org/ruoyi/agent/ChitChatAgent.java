package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 闲聊兜底 Agent
 * 负责问候、日常闲聊和常识性问答,不挂任何工具。
 * 作为 supervisor 的默认落脚点,避免简单任务无子 Agent 可用导致输出为空。
 *
 * @author ageerle@163.com
 */
public interface ChitChatAgent {

    @SystemMessage("""
        你是一个友好、自然的对话助手,负责问候、闲聊和常识性问答。
        要求:
        - 用与用户相同的语言回答,简洁自然
        - 不要编造需要实时数据或专业工具才能得到的事实
        - 如果用户的问题实际需要联网搜索、查数据库、执行技能或生成图表,直接说明这超出你的职责,
          让用户重新描述需求
        """)
    @UserMessage("{{query}}")
    @Agent("闲聊兜底助手:仅用于问候、日常闲聊和不需要联网搜索、数据库、技能或图表的通用问题")
    String chat(@V("query") String query);
}
