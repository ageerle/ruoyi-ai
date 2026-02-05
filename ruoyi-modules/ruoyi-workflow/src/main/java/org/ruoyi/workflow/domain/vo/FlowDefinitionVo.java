package org.ruoyi.workflow.domain.vo;

import lombok.Data;
import org.ruoyi.common.translation.annotation.Translation;
import org.ruoyi.workflow.common.constant.FlowConstant;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程定义视图
 *
 * @author may
 */
@Data
public class FlowDefinitionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 删除标记
     */
    private String delFlag;

    /**
     * 流程定义编码
     */
    private String flowCode;

    /**
     * 流程定义名称
     */
    private String flowName;

    /**
     * 流程分类id
     */
    private String category;

    /**
     * 流程分类名称
     */
    @Translation(type = FlowConstant.CATEGORY_ID_TO_NAME, mapper = "category")
    private String categoryName;

    /**
     * 流程版本
     */
    private String version;

    /**
     * 是否发布（0未发布 1已发布 9失效）
     */
    private Integer isPublish;

    /**
     * 审批表单是否自定义（Y是 N否）
     */
    private String formCustom;

    /**
     * 审批表单路径
     */
    private String formPath;

    /**
     * 流程激活状态（0挂起 1激活）
     */
    private Integer activityStatus;

    /**
     * 监听器类型
     */
    private String listenerType;

    /**
     * 监听器路径
     */
    private String listenerPath;

    /**
     * 扩展字段，预留给业务系统使用
     */
    private String ext;
}
