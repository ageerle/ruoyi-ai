package org.ruoyi.common.core.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务受让人
 *
 * @author AprilWind
 */
@Data
@NoArgsConstructor
public class TaskAssigneeBody implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 权限编码
     */
    private String handlerCode;

    /**
     * 权限名称
     */
    private String handlerName;

    /**
     * 权限分组
     */
    private String groupId;

    /**
     * 开始时间
     */
    private String beginTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 当前页
     */
    private Integer pageNum = 1;

    /**
     * 每页显示条数
     */
    private Integer pageSize = 10;

}
