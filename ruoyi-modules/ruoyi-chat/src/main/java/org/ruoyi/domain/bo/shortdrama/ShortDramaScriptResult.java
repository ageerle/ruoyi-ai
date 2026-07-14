package org.ruoyi.domain.bo.shortdrama;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * Phase 1 剧本打磨结构化响应 —— langchain4j AiServices 自动解析用
 *
 * @author ageerle
 */
@Data
public class ShortDramaScriptResult {

    @Description("项目名称（有吸引力的短剧名）")
    private String projectName;

    @Description("一句话简介（20-50字）")
    private String description;

    @Description("剧本名称")
    private String scriptName;

    @Description("风格基调（如：都市甜宠/古装虐恋/悬疑惊悚/喜剧爽文）")
    private String tone;

    @Description("剧情大纲（400-800字，完整故事线）")
    private String outlineText;

    @Description("完整短剧文本（1000-3000字，标准剧本格式，含场景头、动作描述、对话）")
    private String scriptText;
}
