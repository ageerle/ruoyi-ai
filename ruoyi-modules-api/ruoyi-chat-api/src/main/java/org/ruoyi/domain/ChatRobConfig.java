package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 聊天机器人配置对象 chat_rob_config
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_rob_config")
public class ChatRobConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 所属用户
     */
    private Long userId;

    /**
     * 机器人名称
     */
    private String botName;

    /**
     * 机器唯一码
     */
    private String uniqueKey;

    /**
     * 默认好友回复开关
     */
    private String defaultFriend;

    /**
     * 默认群回复开关
     */
    private String defaultGroup;

    /**
     * 机器人状态  0正常 1启用
     */
    private String enable;

    /**
     * 备注
     */
    private String remark;


}
