package org.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 流程实例业务扩展对象 flow_instance_biz_ext
 *
 * @author may
 * @date 2025-08-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("flow_instance_biz_ext")
public class FlowInstanceBizExt extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
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

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;


}
