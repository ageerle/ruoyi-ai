package org.ruoyi.workflow.domain.vo;

import lombok.Data;
import org.ruoyi.common.translation.annotation.Translation;
import org.ruoyi.common.translation.constant.TransConstant;
import org.dromara.warm.flow.core.entity.User;
import org.ruoyi.workflow.common.constant.FlowConstant;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 任务视图
 *
 * @author may
 */
@Data
public class FlowTaskVo implements Serializable {

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
     * 流程实例表id
     */
    private Long instanceId;

    /**
     * 流程定义名称
     */
    private String flowName;

    /**
     * 业务id
     */
    private String businessId;

    /**
     * 节点编码
     */
    private String nodeCode;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Integer nodeType;

    /**
     * 权限标识 permissionFlag的list形式
     */
    private List<String> permissionList;

    /**
     * 流程用户列表
     */
    private List<User> userList;

    /**
     * 审批表单是否自定义（Y是 N否）
     */
    private String formCustom;

    /**
     * 审批表单
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
     * 流程状态
     */
    private String flowStatus;

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
     * 流程状态
     */
    @Translation(type = TransConstant.DICT_TYPE_TO_LABEL, mapper = "flowStatus", other = "wf_business_status")
    private String flowStatusName;

    /**
     * 办理人类型
     */
    private String type;

    /**
     * 办理人ids
     */
    private String assigneeIds;

    /**
     * 办理人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "assigneeIds")
    private String assigneeNames;

    /**
     * 抄送人id
     */
    private String processedBy;

    /**
     * 抄送人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "processedBy")
    private String processedByName;

    /**
     * 流程签署比例值 大于0为票签，会签
     */
    private BigDecimal nodeRatio;

    /**
     * 申请人id
     */
    private String createBy;

    /**
     * 申请人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "createBy")
    private String createByName;

    /**
     * 是否为申请人节点
     */
    private Boolean applyNode;

    /**
     * 按钮权限
     */
    private List<ButtonPermissionVo> buttonList;

    /**
     * 抄送对象 ID 集合
     * <p>
     * 根据扩展属性中 CopySettingEnum 类型的数据生成，存储需要抄送的对象 ID
     */
    private List<FlowCopyVo> copyList;

    /**
     * 自定义参数 Map
     * <p>
     * 根据扩展属性中 VariablesEnum 类型的数据生成，存储 key=value 格式的自定义参数
     */
    private Map<String, String> varList;

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
