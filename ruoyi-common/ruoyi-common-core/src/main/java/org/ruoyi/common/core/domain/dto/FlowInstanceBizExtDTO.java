package org.ruoyi.common.core.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程实例业务扩展对象
 *
 * @author may
 * @date 2025-08-05
 */
@Data
public class FlowInstanceBizExtDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 流程实例ID
     */
    private Long instanceId;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 业务编码
     */
    private String businessCode;

    /**
     * 业务标题
     */
    private String businessTitle;

}
