package org.ruoyi.system.domain.bo;

import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.ChatAppStore;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用市场业务对象 voice_role
 *
 * @author Lion Li
 * @date 2024-03-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatAppStore.class, reverseConvertGenerate = false)
public class ChatAppStoreBo extends BaseEntity {

    /**
     * id
     */
    @NotNull(message = "id不能为空")
    private Long id;

    /**
     * 角色名称
     */
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 角色描述
     */
    @NotBlank(message = "描述不能为空")
    private String description;

    /**
     * 头像
     */
    @NotBlank(message = "头像不能为空")
    private String avatar;

    /**
     * 音频地址
     */
    @NotBlank(message = "应用地址不能为空")
    private String appUrl;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空")
    private String remark;


}
