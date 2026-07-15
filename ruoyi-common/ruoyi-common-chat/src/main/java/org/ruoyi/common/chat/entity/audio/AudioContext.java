package org.ruoyi.common.chat.entity.audio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

/**
 * Text-to-speech context.
 */
@Data
@Builder
public class AudioContext {

    @NotNull(message = "模型配置不能为空")
    private ChatModelVo chatModelVo;

    @NotBlank(message = "输入文本不能为空")
    private String input;

    private String voice;

    private String responseFormat;

    private Double speed;

    private String instructions;
}
