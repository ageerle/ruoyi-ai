package org.ruoyi.workflow.domain.vo;

import lombok.Data;
import org.ruoyi.common.core.utils.DateUtils;
import org.ruoyi.common.translation.annotation.Translation;
import org.ruoyi.common.translation.constant.TransConstant;
import org.dromara.warm.flow.core.enums.CooperateType;
import org.ruoyi.workflow.common.constant.FlowConstant;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 历史任务视图
 *
 * @author may
 */
@Data
public class FlowHisTaskVo implements Serializable {

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
     * 对应flow_definition表的id
     */
    private Long definitionId;

    /**
     * 流程定义名称
     */
    private String flowName;

    /**
     * 流程实例表id
     */
    private Long instanceId;

    /**
     * 任务表id
     */
    private Long taskId;

    /**
     * 协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)
     */
    private Integer cooperateType;

    /**
     * 协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)
     */
    private String cooperateTypeName;

    /**
     * 业务id
     */
    private String businessId;

    /**
     * 开始节点编码
     */
    private String nodeCode;

    /**
     * 开始节点名称
     */
    private String nodeName;

    /**
     * 开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Integer nodeType;

    /**
     * 目标节点编码
     */
    private String targetNodeCode;

    /**
     * 结束节点名称
     */
    private String targetNodeName;

    /**
     * 审批者
     */
    private String approver;

    /**
     * 审批者
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "approver")
    private String approveName;

    /**
     * 协作人(只有转办、会签、票签、委派)
     */
    private String collaborator;

    /**
     * 权限标识 permissionFlag的list形式
     */
    private List<String> permissionList;

    /**
     * 跳转类型（PASS通过 REJECT退回 NONE无动作）
     */
    private String skipType;

    /**
     * 流程状态
     */
    private String flowStatus;

    /**
     * 任务状态
     */
    private String flowTaskStatus;

    /**
     * 流程状态
     */
    private String flowStatusName;

    /**
     * 审批意见
     */
    private String message;

    /**
     * 业务详情 存业务类的json
     */
    private String ext;

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

    /**
     * 审批表单是否自定义（Y是 N否）
     */
    private String formCustom;

    /**
     * 审批表单路径
     */
    private String formPath;

    /**
     * 流程定义编码
     */
    private String flowCode;

    /**
     * 流程版本号
     */
    private String version;

    /**
     * 运行时长
     */
    private String runDuration;

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

    /**
     * 设置创建时间并计算任务运行时长
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
        updateRunDuration();
    }

    /**
     * 设置更新时间并计算任务运行时长
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        updateRunDuration();
    }

    /**
     * 更新运行时长
     */
    private void updateRunDuration() {
        // 如果创建时间和更新时间均不为空，计算它们之间的时长
        if (this.updateTime != null && this.createTime != null) {
            this.runDuration = DateUtils.getTimeDifference(this.updateTime, this.createTime);
        }
    }

    /**
     * 设置协作方式，并通过协作方式获取名称
     */
    public void setCooperateType(Integer cooperateType) {
        this.cooperateType = cooperateType;
        this.cooperateTypeName = CooperateType.getValueByKey(cooperateType);
    }

}
