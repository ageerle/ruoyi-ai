package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 微信机器人对象 wx_rob_config
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_rob_config")
public class WxRobConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 用户id
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
     * 备注（微信号）
     */
    private String remark;

    /**
     * 默认好友回复开关
     */
    private String defaultFriend;

    /**
     * 默认群回复开关
     */
    private String defaultGroup;


    /**
     * 机器启用1禁用0
     */
    private String enable;

}
