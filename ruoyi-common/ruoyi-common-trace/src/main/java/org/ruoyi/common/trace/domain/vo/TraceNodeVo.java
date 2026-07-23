package org.ruoyi.common.trace.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.trace.domain.TraceNode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 链路追踪节点记录视图对象。
 * <p>
 * 除实体映射字段外，还提供前端可直接展示的显示标签和解析后的 payload 对象。
 */
@Data
@AutoMapper(target = TraceNode.class)
public class TraceNodeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    /** 原始 input payload 字符串，parsedInput 解析失败时回退使用 */
    private String inputPayload;

    /** 原始 output payload 字符串，parsedOutput 解析失败时回退使用 */
    private String outputPayload;

    /** 原始 metadata 字符串，parsedMetadata 解析失败时回退使用 */
    private String metadata;

    // ======================== 展示用计算字段 ========================

    /** 节点类型中文标签，如 "知识检索"、"LLM 调用" */
    @JsonProperty("nodeTypeLabel")
    private String nodeTypeLabel;

    /** 状态中文标签，如 "成功"、"失败"、"运行中" */
    @JsonProperty("statusLabel")
    private String statusLabel;

    /** 节点展示名称（中文友好），从 nodeName 转换 */
    @JsonProperty("nodeDisplayName")
    private String nodeDisplayName;

    // ======================== 解析后的 payload ========================

    /** input payload 解析为 Map，前端可直接读取结构化字段 */
    @JsonProperty("parsedInput")
    private Map<String, Object> parsedInput;

    /** output payload 解析为 Map，前端可直接读取结构化字段 */
    @JsonProperty("parsedOutput")
    private Map<String, Object> parsedOutput;

    /** metadata 解析为 Map */
    @JsonProperty("parsedMetadata")
    private Map<String, Object> parsedMetadata;
}
