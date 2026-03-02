package org.ruoyi.common.chat.entity.chat;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 模型管理对象 chat_model
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_model")
public class ChatModel extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 模型分类
     */
    private String category;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型供应商
     */
    private String providerCode;

    /**
     * 模型描述
     */
    private String modelDescribe;

    /**
     * 模型价格
     */
    private Long modelPrice;

    /**
     * 计费类型
     */
    private String modelType;

    /**
     * 是否显示
     */
    private String modelShow;

    /**
     * 是否免费
     */
    private String modelFree;

    /**
     * 模型优先级(值越大优先级越高)
     */
    private Long priority;

    /**
     * 请求地址
     */
    private String apiHost;

    /**
     * 密钥
     */
    private String apiKey;

    /**
     * 备注
     */
    private String remark;


}
