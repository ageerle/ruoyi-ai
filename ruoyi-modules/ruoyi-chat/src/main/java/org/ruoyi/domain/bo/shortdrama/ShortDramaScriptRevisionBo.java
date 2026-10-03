package org.ruoyi.domain.bo.shortdrama;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShortDramaScriptRevisionBo {

    @NotBlank(message = "修改意见不能为空")
    @Size(max = 10000, message = "修改意见不能超过10000字")
    private String instruction;
}
