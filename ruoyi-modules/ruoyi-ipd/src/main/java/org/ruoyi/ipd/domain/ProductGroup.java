package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * IPD 产品组（组织架构）——TS-05 product_groups
 */
@Data
@Builder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_groups", autoResultMap = true)
public class ProductGroup extends BaseEntity {

    @TableId
    private Long id;

    /** 产品组名称 */
    private String groupName;

    /** 组长（随 HR API 同步） */
    private Long leaderPersonId;

    /** 上级组（预留） */
    private Long parentId;

    /** 描述 */
    private String description;

    /** 租户ID */
    private String tenantId;

    /** 删除标志（0正常 1删除） */
    private String delFlag;
}