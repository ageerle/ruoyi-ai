package org.ruoyi.asset.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 最低使用年限表 min_usage_period
 *
 * @author cass
 * @date 2025-09-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("min_usage_period")
public class MinUsagePeriod extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 固定资产类别
     */
    private String category;

    /**
     * 内容
     */
    private String content;

    /**
     * 最低使用年限（年）
     */
    private Integer minYears;

    /**
     * 国标代码
     */
    private String gbCode;

}
