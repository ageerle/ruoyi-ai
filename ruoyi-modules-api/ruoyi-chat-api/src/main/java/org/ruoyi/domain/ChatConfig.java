package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.common.sensitive.annotation.Sensitive;
import org.ruoyi.common.sensitive.core.SensitiveStrategy;

import java.io.Serial;

/**
 * 对话配置信息对象 chat_config
 *
 * @author Lion Li
 * @date 2024-04-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_config")
public class ChatConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 配置类型
     */
    private String category;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置值
     */
    @Sensitive(strategy = SensitiveStrategy.SKY)
    private String configValue;

    /**
     * 说明
     */
    private String configDict;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 更新IP
     */
    private String updateIp;


}
