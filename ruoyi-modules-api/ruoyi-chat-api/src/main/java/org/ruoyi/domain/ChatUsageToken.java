package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户token使用详情对象 chat_usage_token
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@TableName("chat_usage_token")
public class ChatUsageToken implements Serializable {

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
