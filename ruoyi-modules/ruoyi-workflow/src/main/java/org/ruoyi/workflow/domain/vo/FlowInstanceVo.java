package org.ruoyi.workflow.domain.vo;

import lombok.Data;
import org.ruoyi.common.translation.annotation.Translation;
import org.ruoyi.common.translation.constant.TransConstant;
import org.ruoyi.workflow.common.constant.FlowConstant;

import java.util.Date;

/**
 * 流程实例视图
 *
 * @author may
 */
@Data
public class FlowInstanceVo {

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
     * 对应flow_definition表的id
     */
    private Long definitionId;

    /**
     * 流程定义名称
     */
    private String flowName;

    /**
     * 流程定义编码
     */
    private String flowCode;

    /**
     * 业务id
     */
    private String businessId;

    /**
     * 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Integer nodeType;

    /**
     * 流程节点编码   每个流程的nodeCode是唯一的,即definitionId+nodeCode唯一,在数据库层面做了控制
     */
    private String nodeCode;

    /**
     * 流程节点名称
     */
    private String nodeName;

    /**
     * 流程变量
     */
    private String variable;

    /**
     * 流程状态（0待提交 1审批中 2 审批通过 3自动通过 8已完成 9已退回 10失效）
     */
    private String flowStatus;

    /**
     * 流程状态
     */
    private String flowStatusName;

    /**
     * 流程激活状态（0挂起 1激活）
     */
    private Integer activityStatus;

    /**
     * 审批表单是否自定义（Y是 N否）
     */
    private String formCustom;

    /**
     * 审批表单路径
     */
    private String formPath;

    /**
     * 扩展字段，预留给业务系统使用
     */
    private String ext;

    /**
     * 流程定义版本
     */
    private String version;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 申请人
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "createBy")
    private String createByName;

    /**
     * 流程分类id
     */
    private String category;

    /**
     * 流程分类名称
     */
    @Translation(type = FlowConstant.CATEGORY_ID_TO_NAME, mapper = "category")
    private String categoryName;

    //业务扩展信息开始
    /**
     * 业务编码
     */
    private String businessCode;

    /**
     * 业务标题
     */
    private String businessTitle;
    //业务扩展信息结束

}
