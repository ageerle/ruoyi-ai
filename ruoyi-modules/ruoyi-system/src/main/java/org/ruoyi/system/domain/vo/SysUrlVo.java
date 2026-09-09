package org.ruoyi.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.SysUrl;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * URL 管理视图对象 sys_url
 *
 * @author ruoyi
 */
@Data
@AutoMapper(target = SysUrl.class)
public class SysUrlVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 链接ID
     */
    private Long urlId;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 链接名称
     */
    private String name;

    /**
     * HTTP(S) 地址
     */
    private String url;

    /**
     * 链接说明
     */
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
