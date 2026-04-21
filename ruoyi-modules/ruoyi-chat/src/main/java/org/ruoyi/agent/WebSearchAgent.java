package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 浏览器工具 Agent
 * 能够操作浏览器相关工具：网络搜索、网页抓取、浏览器自动化等
 *
 * @author ageerle@163.com
 * @date 2025/04/10
 */
public interface WebSearchAgent {

    @SystemMessage("""
        你是一个系统工具助手，能够使用工具来帮助用户获取信息和操作浏览器。

        【最重要原则】
        除非用户明确要求使用浏览器查询信息，否则不要主动调用任何搜索或浏览器工具。
        使用指南：
        - 搜索信息时使用 bing_search
        - 需要详细网页内容时使用 crawl_webpage
        - 需要交互操作（登录、点击、填写表单）时使用 Playwright 工具
        - 在回答中注明信息来源
        """)
    @UserMessage("{{query}}")
    @Agent("浏览器工具助手，支持网络搜索、网页抓取和浏览器自动化操作")
    String search(@V("query") String query);
}
