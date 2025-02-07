package org.ruoyi.fusion.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author WangLe
 */
@Data
@ApiModel("Discord账号")
public class InsightFace implements Serializable {
    /**本人头像json*/
    @ApiModelProperty("本人头像json")
    private String sourceBase64;

    /**明星头像json*/
    @ApiModelProperty("明星头像json")
    private String targetBase64;
}
