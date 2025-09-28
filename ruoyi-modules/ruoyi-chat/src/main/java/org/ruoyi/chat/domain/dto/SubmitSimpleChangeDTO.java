package org.ruoyi.chat.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@Schema(name = "变化任务提交参数-simple")
@EqualsAndHashCode(callSuper = true)
public class SubmitSimpleChangeDTO extends BaseSubmitDTO {

	@Schema(description = "变化描述: ID $action$index", requiredMode = Schema.RequiredMode.REQUIRED, example = "1320098173412546 U2")
	private String content;

}
