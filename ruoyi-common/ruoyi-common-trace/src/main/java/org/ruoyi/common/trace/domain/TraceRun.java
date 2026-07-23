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
 * 链路追踪运行记录 trace_run。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trace_run")
public class TraceRun extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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
    @TableLogic
    private String delFlag;
}
