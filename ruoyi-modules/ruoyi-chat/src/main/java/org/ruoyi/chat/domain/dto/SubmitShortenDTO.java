package org.ruoyi.chat.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "prompt分析提交参数")
public class SubmitShortenDTO extends BaseSubmitDTO {

    private String botType;

    private String prompt;

}
