package org.ruoyi.domain.entity.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

/**
 * 智能体信息实体
 * <p>
 * 一个智能体聚合：一个聊天模型 + 一组 MCP 工具 + 一组磁盘技能 + 一组知识库 + 自定义提示词
 * 关联以 JSON 数组字符串列存储：mcp_tool_ids / skill_names / knowledge_ids
 *
 * @author ruoyi team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_info")
public class Agent extends TenantEntity {

    /**
     * 智能体ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 智能体名称
     */
    private String agentName;

    /**
     * 智能体描述（下拉展示用）
     */
    private String agentDescribe;

    /**
     * 展示图标/头像URL
     */
    private String agentShow;

    /**
     * 绑定的聊天模型ID（chat_model.id, category=chat）
     */
    private Long modelId;

    /**
     * 是否启用深度思考(ReAct多子Agent)：0 否 1 是
     */
    private String enableThinking;

    /**
     * 自定义系统提示词
     */
    private String systemPrompt;

    /**
     * 关联MCP工具ID列表（JSON数组，[Long]）
     */
    private String mcpToolIds;

    /**
     * 关联磁盘技能名列表（JSON数组，[String]）
     */
    private String skillNames;

    /**
     * 关联知识库ID列表（JSON数组，[Long]）
     */
    private String knowledgeIds;

    /**
     * 状态：0 正常 1 停用
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
