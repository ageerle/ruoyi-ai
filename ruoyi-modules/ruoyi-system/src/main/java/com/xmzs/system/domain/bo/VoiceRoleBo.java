package com.xmzs.system.domain.bo;

import com.xmzs.common.mybatis.core.domain.BaseEntity;
import com.xmzs.system.domain.VoiceRole;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 配音角色业务对象 voice_role
 *
 * @author Lion Li
 * @date 2024-03-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = VoiceRole.class, reverseConvertGenerate = false)
public class VoiceRoleBo extends BaseEntity {

    /**
     * id
     */
    @NotNull(message = "id不能为空")
    private Long id;

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    private String name;

    /**
     * 角色描述
     */
    @NotBlank(message = "角色描述不能为空")
    private String description;

    /**
     * 头像
     */
    @NotBlank(message = "头像不能为空")
    private String avatar;

    /**
     * 角色id
     */
    @NotBlank(message = "角色id不能为空")
    private String voiceId;

    /**
     * 音频地址
     */
    @NotBlank(message = "音频地址不能为空")
    private String fileUrl;

    /**
     * 音频预处理（实验性）
     */
    @NotBlank(message = "音频预处理（实验性）不能为空")
    private String preProcess;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空")
    private String remark;


}
