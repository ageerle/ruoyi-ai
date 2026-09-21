package org.ruoyi.common.chat.domain.bo.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * 按厂商批量更新密钥业务对象
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelBatchKeyBo extends BaseEntity {

    /**
     * 模型供应商
     */
    @NotBlank(message = "厂商编码不能为空")
    private String providerCode;

    /**
     * 密钥
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "密钥不能为空")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String apiKey;

}
