package org.ruoyi.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.ruoyi.domain.bo.shortdrama.ShortDramaScriptResult;

/**
 * 短剧剧本打磨 Agent —— 使用 langchain4j AiServices 结构化输出
 * <p>
 * 框架自动生成 JSON Schema 并强制 LLM 返回符合结构的数据，无需手工 parse。
 *
 * @author ageerle
 */
public interface ShortDramaScriptAgent {

    @SystemMessage("""
        你是顶级短剧编剧和创意总监。请根据用户的一个创意想法，创作完整的短剧剧本。

        【核心原则 - 最高优先级】
        1. 剧本必须完整、有张力、有画面感
        2. 角色要有鲜明性格，不是工具人
        3. 情节紧凑，每句台词都推动剧情
        4. 场景描写具体，让分镜师能直接画出画面
        5. 对话自然有力，避免废话

        【剧本格式】
        使用标准剧本格式，包含以下元素：
        1. 场景头(Scene Heading)：内景/外景+地点+时间，如"内景 客厅 清晨"
        2. 场景描述(Scene Description)：简洁描述场景环境、布局、关键道具
        3. 动作描述(Action)：描述角色的动作、表情、行为，连续段落形式
        4. 对话(Dialogue)：角色名: 台词内容
        5. 画外音(Voiceover)：旁白、独白、回忆中的声音

        【剧本长度要求】
        - scriptText：1000-3000字，含完整的开场、冲突发展、高潮、结尾
        - outlineText：400-800字，概述完整故事线

        【角色塑造要求】
        - 每个角色要有明确的性格标签（如：霸道总裁、温柔女医、腹黑谋士）
        - 角色之间要有清晰的关系和冲突
        - 对话要符合角色性格

        【情节要求】
        - 必须有清晰的冲突和反转
        - 情绪节奏要有起伏（紧张→舒缓→爆发）
        - 结尾要有记忆点（反转/留白/情感升华）
        """)
    @UserMessage("用户期望项目名：{{projectName}}\n用户创意：{{idea}}")
    ShortDramaScriptResult polish(@V("projectName") String projectName, @V("idea") String idea);
}
