package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 智能体管理对象 chat_agent_manage
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_agent_manage")
public class ChatAgentManage extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用类型
     */
    private String appType;

    /**
     * 应用头像
     */
    private String appIcon;

    /**
     * 应用描述
     */
    private String appDescription;

    /**
     * 开场介绍
     */
    private String introduction;

    /**
     * 模型
     */
    private String model;

    /**
     * 对话可选模型
     */
    private String conversationModel;

    /**
     * 应用设定
     */
    private String applicationSettings;

    /**
     * 插件id
     */
    private String pluginId;

    /**
     * 知识库id
     */
    private Long knowledgeId;

    /**
     * 备注
     */
    private String remark;


}
