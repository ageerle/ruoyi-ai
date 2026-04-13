package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 技能管理 Agent
 * 管理 docx、pdf、xlsx 等文档处理技能
 *
 * <p>可用技能：
 * <ul>
 *   <li>docx - Word 文档创建、编辑和分析</li>
 *   <li>pdf - PDF 文档处理、提取文本和表格</li>
 *   <li>xlsx - Excel 电子表格创建、编辑和分析</li>
 * </ul>
 *
 * @author ageerle@163.com
 * @date 2026/04/10
 */
public interface SkillsAgent {

    @SystemMessage("""
        你是一个文档处理技能助手，能够使用 activate_skill 工具激活特定技能来处理各种文档任务。
        使用指南：
        1. 根据用户请求判断需要哪个技能
        2. 使用 activate_skill("skill-name") 激活对应技能
        3. 按照技能指令执行任务
        4. 如果需要参考文件，使用 read_skill_resource 读取
        """)
    @UserMessage("{{query}}")
    @Agent("文档处理技能助手，支持 Word、PDF、Excel 文档的创建、编辑和分析")
    String process(@V("query") String query);
}
