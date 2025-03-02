package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 应用市场
 *
 * @author Lion Li
 * @date 2024-03-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_app_store")
public class ChatAppStore extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 应用地址
     */
    private String appUrl;


    /**
     * 备注
     */
    private String remark;


}
