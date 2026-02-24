package org.ruoyi.common.chat.domain.entity.image;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

/**
 * 文生图对话上下文对象
 *
 * @author zengxb
 * @date 2026-02-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class ImageContext {

    /**
     * 模型管理视图对象
     */
    @NotNull(message = "模型管理视图对象不能为空")
    private ChatModelVo chatModelVo;

    /**
     * 提示词
     */
    @NotNull(message = "提示词不能为空")
    private String prompt;

    /**
     * 图片尺寸大小
     */
    private String size;

    /**
     * 随机数种子
     */
    @Min(value = 0, message = "随机数种子不能小于0")
    @Max(value = 2147483647, message = "随机数种子不能大于2147483647")
    private Integer seed;
}
