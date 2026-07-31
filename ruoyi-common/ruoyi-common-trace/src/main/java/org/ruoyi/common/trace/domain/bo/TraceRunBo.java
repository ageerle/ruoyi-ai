package org.ruoyi.common.trace.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.common.trace.domain.TraceRun;

import java.util.Date;

/**
 * 链路追踪运行记录查询对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = TraceRun.class, reverseConvertGenerate = false)
public class TraceRunBo extends BaseEntity {

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
    private String metadata;
}
