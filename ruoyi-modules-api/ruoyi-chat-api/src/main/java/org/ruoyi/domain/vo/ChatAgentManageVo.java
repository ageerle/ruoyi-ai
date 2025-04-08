package org.ruoyi.system.domain.vo;

import org.ruoyi.system.domain.ChatAgentManage;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import annotation.excel.common.org.ruoyi.ExcelDictFormat;
import convert.excel.common.org.ruoyi.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;



/**
 * 智能体管理视图对象 chat_agent_manage
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatAgentManage.class)
public class ChatAgentManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @ExcelProperty(value = "主键id")
    private Long id;

    /**
     * 应用名称
     */
    @ExcelProperty(value = "应用名称")
    private String appName;

    /**
     * 应用类型
     */
    @ExcelProperty(value = "应用类型")
    private String appType;

    /**
     * 应用头像
     */
    @ExcelProperty(value = "应用头像")
    private String appIcon;

    /**
     * 应用描述
     */
    @ExcelProperty(value = "应用描述")
    private String appDescription;

    /**
     * 开场介绍
     */
    @ExcelProperty(value = "开场介绍")
    private String introduction;

    /**
     * 模型
     */
    @ExcelProperty(value = "模型")
    private String model;

    /**
     * 对话可选模型
     */
    @ExcelProperty(value = "对话可选模型")
    private String conversationModel;

    /**
     * 应用设定
     */
    @ExcelProperty(value = "应用设定")
    private String applicationSettings;

    /**
     * 插件id
     */
    @ExcelProperty(value = "插件id")
    private String pluginId;

    /**
     * 知识库id
     */
    @ExcelProperty(value = "知识库id")
    private Long knowledgeId;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
