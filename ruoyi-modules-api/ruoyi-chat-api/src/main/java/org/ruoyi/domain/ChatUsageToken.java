package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 用户token使用详情对象 chat_usage_token
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_usage_token")
public class ChatUsageToken extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 用户
     */
    private Long userId;

    /**
     * 待结算token
     */
    private Integer token;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 累计使用token
     */
    private String totalToken;


}
