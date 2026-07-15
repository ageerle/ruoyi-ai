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

    private String videoId;
}
