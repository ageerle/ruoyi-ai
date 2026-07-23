package org.ruoyi.common.trace.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 链路追踪节点记录 trace_node。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trace_node")
public class TraceNode extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;
    private String traceId;
    private String nodeId;
    private String parentNodeId;
    private String nodeName;
    private String nodeType;
    private Integer depth;
    private Integer sortOrder;
    private String className;
    private String methodName;
    private String status;
    private Date startTime;
    private Date endTime;
    private Long durationMs;
    private String errorMessage;
    private String inputPayload;
    private String outputPayload;
    private String metadata;
    @TableLogic
    private String delFlag;
}
