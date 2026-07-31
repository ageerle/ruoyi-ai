package org.ruoyi.domain.vo.agent;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 智能体视图对象
 * <p>
 * 注意：不使用 @AutoMapper，因为 entity 的 mcpToolIds/skillNames/knowledgeIds 是 JSON 字符串列，
 * 而 VO 是 List 类型，MapStruct 无法双向自动转换。由 AgentServiceImpl.toVo 手动组装。
 *
 * @author ruoyi team
 */
@Data
@ExcelIgnoreUnannotated
public class AgentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 智能体ID
     */
    @ExcelProperty(value = "智能体ID")
    private Long id;

    /**
     * 智能体名称
     */
    @ExcelProperty(value = "智能体名称")
    private String agentName;

    /**
     * 智能体描述
     */
    @ExcelProperty(value = "智能体描述")
    private String agentDescribe;

    /**
     * 展示图标/头像URL
     */
    private String agentShow;

    /**
     * 绑定的聊天模型ID
     */
    @ExcelProperty(value = "绑定模型ID")
    private Long modelId;

    /**
     * 绑定的聊天模型名称（关联展示）
     */
    @ExcelProperty(value = "绑定模型")
    private String modelName;

    /**
     * 是否启用深度思考：0 否 1 是
     */
    @ExcelProperty(value = "深度思考")
    private String enableThinking;

    /**
     * 自定义系统提示词
     */
    private String systemPrompt;

    /**
     * 关联MCP工具ID列表
     */
    private List<Long> mcpToolIds;

    /**
     * 关联MCP工具名称列表（关联展示）
     */
    private List<String> mcpToolNames;

    /**
     * 关联磁盘技能名列表
     */
    private List<String> skillNames;

    /**
     * 关联知识库ID列表
     */
    private List<Long> knowledgeIds;

    /**
     * 关联知识库名称列表（关联展示）
     */
    private List<String> knowledgeNames;

    /**
     * 状态：0 正常 1 停用
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

}
