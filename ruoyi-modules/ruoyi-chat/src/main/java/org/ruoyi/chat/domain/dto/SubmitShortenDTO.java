package org.ruoyi.chat.domain.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("prompt分析提交参数")
public class SubmitShortenDTO extends BaseSubmitDTO{

	private String botType;

    private String prompt;

}
