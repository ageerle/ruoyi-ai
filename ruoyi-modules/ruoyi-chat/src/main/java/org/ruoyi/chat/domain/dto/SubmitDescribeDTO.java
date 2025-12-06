package org.ruoyi.chat.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Schema(name = "Describe提交参数")
@EqualsAndHashCode(callSuper = true)
public class SubmitDescribeDTO extends BaseSubmitDTO {

    @Schema(description = "图片base64", requiredMode = Schema.RequiredMode.REQUIRED, example = "data:image/png;base64,xxx")
    private String base64;
}
