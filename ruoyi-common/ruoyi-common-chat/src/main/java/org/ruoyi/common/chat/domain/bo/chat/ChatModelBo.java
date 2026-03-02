package org.ruoyi.common.chat.domain.bo.chat;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.chat.entity.chat.ChatModel;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * 模型管理业务对象 chat_model
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatModel.class, reverseConvertGenerate = false)
public class ChatModelBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
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
