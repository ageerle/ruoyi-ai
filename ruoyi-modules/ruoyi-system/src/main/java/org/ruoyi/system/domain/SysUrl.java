package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.ruoyi.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * URL 管理表 sys_url
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_url")
public class SysUrl extends TenantEntity {

    /**
     * 链接ID
     */
    @TableId(value = "url_id")
    private Long urlId;

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

}
