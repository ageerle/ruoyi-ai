package org.ruoyi.workflow.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 抄送
 *
 * @author may
 */
@Data
public class FlowCopyBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

}
