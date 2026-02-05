package org.ruoyi.common.core.domain.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 总体流程监听
 *
 * @author may
 */
@Data
public class ProcessEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 流程定义编码
     */
    private String flowCode;

    /**
     * 实例id
     */
    private Long instanceId;

    /**
     * 业务id
     */
    private String businessId;

    /**
     * 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Integer nodeType;

    /**
     * 流程节点编码
     */
    private String nodeCode;

    /**
     * 流程节点名称
     */
    private String nodeName;

    /**
     * 流程状态
     */
    private String status;

    /**
     * 办理参数
     */
    private Map<String, Object> params;

    /**
     * 当为true时为申请人节点办理
     */
    private Boolean submit;

}
