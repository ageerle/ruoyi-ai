package org.ruoyi.common.chat.entity.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

/**
 * Text-to-video context.
 */
@Data
@Builder
public class VideoContext {

    @NotNull(message = "模型配置不能为空")
    private ChatModelVo chatModelVo;

    @NotBlank(message = "提示词不能为空")
    private String prompt;

    private String size;

    private Integer seconds;

    private String quality;

    /** 参考图 URL（图生视频模式，单图） */
    private String imageUrl;

    /** 多参考图 URL（多参考图生视频模式，配合 @imageN prompt 使用） */
    private java.util.List<String> referenceImages;

    /** 参考音频 URL 列表（对白口型对齐模式） */
    private java.util.List<String> referenceAudios;

    /** 是否让模型生成同步音频（环境音/动效） */
    private Boolean generateAudio;

    /** 是否要求返回末帧，用于下一镜首帧承接 */
    private Boolean returnLastFrame;

    /** 上一镜末帧 URL（同场景连续镜头首帧承接用，作为额外参考图传入） */
    private String lastFrameUrl;

    private String videoId;
}
