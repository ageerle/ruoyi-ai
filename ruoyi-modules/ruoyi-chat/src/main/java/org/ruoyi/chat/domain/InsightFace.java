package org.ruoyi.chat.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author WangLe
 */
@Data
@Schema(name = "Discord账号")
public class InsightFace implements Serializable {
    /**本人头像json*/
    @Schema(description = "本人头像json")
    private String sourceBase64;

    /**明星头像json*/
    @Schema(description = "明星头像json")
    private String targetBase64;
}
