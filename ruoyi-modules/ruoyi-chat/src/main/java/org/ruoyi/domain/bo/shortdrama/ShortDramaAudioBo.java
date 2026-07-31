package org.ruoyi.domain.bo.shortdrama;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.shortdrama.ShortDramaAudio;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ShortDramaAudio.class, reverseConvertGenerate = false)
public class ShortDramaAudioBo extends BaseEntity {

    private Long id;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotBlank(message = "语音资产名称不能为空")
    private String name;

    @NotBlank(message = "语音类型不能为空")
    @Pattern(regexp = "narration|dialogue", message = "语音类型只能是 narration 或 dialogue")
    private String audioType;

    @NotBlank(message = "语音文案不能为空")
    private String text;

    /** 音色（生成语音时使用，可空，空则用模型默认） */
    private String voice;

    /** 对白关联的分镜ID（旁白类型留空） */
    private Long linkedStoryboardId;
}
