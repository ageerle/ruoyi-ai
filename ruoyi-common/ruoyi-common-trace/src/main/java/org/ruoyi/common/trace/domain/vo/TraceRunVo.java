package org.ruoyi.common.trace.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.trace.domain.TraceRun;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 链路追踪运行记录视图对象。
 * <p>
 * 除实体映射字段外，还提供前端可直接展示的显示标签和解析后的 metadata 对象。
 */
@Data
@AutoMapper(target = TraceRun.class)
public class TraceRunVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String traceId;
    private String traceName;
    private String businessType;
    private String businessId;
    private Long userId;
    private String tenantId;
    private String status;
    private Date startTime;
    private Date endTime;
    private Long durationMs;
    private String errorMessage;

    /** 原始 metadata 字符串（兼容旧版），新代码请使用 parsedMetadata */
    private String metadata;

    // ======================== 展示用计算字段 ========================

    /** 状态中文标签，如 "成功"、"失败"、"运行中" */
    @JsonProperty("statusLabel")
    private String statusLabel;

    /** 业务类型中文标签，如 "RAG 对话" */
    @JsonProperty("businessTypeLabel")
    private String businessTypeLabel;

    /** metadata 解析为 Map */
    @JsonProperty("parsedMetadata")
    private Map<String, Object> parsedMetadata;
}
