package org.ruoyi.common.chat.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @sine 2023-04-08
 */
@Data
public class Dall3Request {

    @NotEmpty(message = "传入的模型不能为空")
    private String model;

    @NotEmpty(message = "提示词不能为空")
    private String prompt;

    /** 图片大小 */
    @NotEmpty(message = "图片大小不能为空")
    private String size ;

    /** 图片质量 */
    @NotEmpty(message = "图片质量不能为空")
    private String quality;

    /** 图片风格 */
    @NotEmpty(message = "图片风格不能为空")
    private String style;

}
