package org.ruoyi.common.chat.domain.bo.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.ruoyi.common.chat.entity.chat.ChatModel;
import org.ruoyi.common.chat.security.ChatModelCredentialPolicy;
import org.ruoyi.common.chat.security.ChatModelSecretReference;
import org.ruoyi.common.core.validate.AddGroup;
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
    @NotBlank(message = "模型分类不能为空", groups = { AddGroup.class, EditGroup.class })
    private String category;

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String modelName;

    /**
     * 模型供应商
     */
    @NotBlank(message = "模型供应商不能为空", groups = { AddGroup.class, EditGroup.class })
    private String providerCode;

    /**
     * 模型描述
     */
    private String modelDescribe;

    /**
     * 是否显示
     */
    private String modelShow;

    /**
     * 向量维度
     */
    private Integer modelDimension;

    /**
     * 请求地址
     */
    @NotBlank(message = "请求地址不能为空", groups = { AddGroup.class, EditGroup.class })
    @Pattern(regexp = ChatModelCredentialPolicy.HTTPS_API_HOST_REGEXP,
        message = "请求地址必须是无凭据、查询参数和片段的 HTTPS 地址",
        groups = { AddGroup.class, EditGroup.class })
    private String apiHost;

    /**
     * 密钥
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Pattern(regexp = ChatModelSecretReference.ENV_REFERENCE_REGEXP,
        message = "密钥必须使用受信任的环境变量引用",
        groups = { AddGroup.class, EditGroup.class })
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String apiKey;

    /**
     * 备注
     */
    private String remark;


}
