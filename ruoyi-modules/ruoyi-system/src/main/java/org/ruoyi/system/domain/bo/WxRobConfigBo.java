package org.ruoyi.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.WxRobConfig;

/**
 * 微信机器人业务对象 wx_rob_config
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WxRobConfig.class, reverseConvertGenerate = false)
public class WxRobConfigBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
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

    /**
     * 备注
     */
    private String remark;

}
