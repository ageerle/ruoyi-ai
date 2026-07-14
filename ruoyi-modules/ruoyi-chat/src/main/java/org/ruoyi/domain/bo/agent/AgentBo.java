package org.ruoyi.domain.bo.agent;

import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.agent.Agent;

import java.io.Serial;
import java.util.List;

/**
 * 智能体业务对象
 *
 * @author ruoyi team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = Agent.class, reverseConvertGenerate = false)
public class AgentBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 智能体ID
     */
    private Long id;

    /**
     * 智能体名称
     */
    @NotBlank(message = "智能体名称不能为空")
    @Size(min = 0, max = 200, message = "智能体名称不能超过{max}个字符")
    private String agentName;

    /**
     * 智能体描述
     */
    private String agentDescribe;

    /**
     * 展示图标/头像URL
     */
    private String agentShow;

    /**
     * 绑定的聊天模型ID
     */
    @NotNull(message = "绑定模型不能为空")
    private Long modelId;

    /**
     * 是否启用深度思考：0 否 1 是
     */
    private String enableThinking;

    /**
     * 自定义系统提示词
     */
    private String systemPrompt;

    /**
     * 关联MCP工具ID列表
     */
    @AutoMapping(target = "mcpToolIds", expression = "java(org.ruoyi.common.json.utils.JsonUtils.toJsonString(source.getMcpToolIds()))")
    private List<Long> mcpToolIds;

    /**
     * 关联磁盘技能名列表
     */
    @AutoMapping(target = "skillNames", expression = "java(org.ruoyi.common.json.utils.JsonUtils.toJsonString(source.getSkillNames()))")
    private List<String> skillNames;

    /**
     * 关联知识库ID列表
     */
    @AutoMapping(target = "knowledgeIds", expression = "java(org.ruoyi.common.json.utils.JsonUtils.toJsonString(source.getKnowledgeIds()))")
    private List<Long> knowledgeIds;

    /**
     * 状态：0 正常 1 停用
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
