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

    /** Atlas seed-audio 参考资源：[{speaker, audioUrl, audioData, imageData}]，speaker 为音色名 */
    private java.util.List<java.util.Map<String, String>> references;

    /** Atlas seed-audio 采样率 */
    private Integer sampleRate;

    /** Atlas seed-audio 音调调整 (-12~12) */
    private Integer pitchRate;

    /** Atlas seed-audio 语速调整 (-50~100) */
    private Integer speechRate;

    /** Atlas seed-audio 响度调整 (-50~100) */
    private Integer loudnessRate;
}
