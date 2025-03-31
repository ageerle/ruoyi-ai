package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 插件管理对象 chat_plugin
 *
 * @author ageerle
 * @date 2025-03-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_plugin")
public class ChatPlugin extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 插件名称
     */
    private String name;

    /**
     * 插件编码
     */
    private String code;

    /**
     * 备注
     */
    private String remark;


}
