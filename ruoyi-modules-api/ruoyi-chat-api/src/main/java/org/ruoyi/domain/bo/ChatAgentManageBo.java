package org.ruoyi.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.system.domain.ChatAgentManage;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 智能体管理业务对象 chat_agent_manage
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatAgentManage.class, reverseConvertGenerate = false)
public class ChatAgentManageBo extends BaseEntity {

    /**
     * 主键id
     */
    @NotNull(message = "主键id不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String appName;

    /**
     * 应用类型
     */
    @NotBlank(message = "应用类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String appType;

    /**
     * 应用头像
     */
    @NotBlank(message = "应用头像不能为空", groups = { AddGroup.class, EditGroup.class })
    private String appIcon;

    /**
     * 应用描述
     */
    @NotBlank(message = "应用描述不能为空", groups = { AddGroup.class, EditGroup.class })
    private String appDescription;

    /**
     * 开场介绍
     */
    @NotBlank(message = "开场介绍不能为空", groups = { AddGroup.class, EditGroup.class })
    private String introduction;

    /**
     * 模型
     */
    @NotBlank(message = "模型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String model;

    /**
     * 对话可选模型
     */
    @NotBlank(message = "对话可选模型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String conversationModel;

    /**
     * 应用设定
     */
    @NotBlank(message = "应用设定不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationSettings;

    /**
     * 插件id
     */
    @NotBlank(message = "插件id不能为空", groups = { AddGroup.class, EditGroup.class })
    private String pluginId;

    /**
     * 知识库id
     */
    @NotNull(message = "知识库id不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long knowledgeId;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;


}
