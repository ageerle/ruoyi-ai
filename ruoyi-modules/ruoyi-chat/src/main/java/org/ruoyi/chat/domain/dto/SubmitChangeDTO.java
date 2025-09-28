package org.ruoyi.chat.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.chat.enums.TaskAction;


@Data
@Schema(name = "变化任务提交参数")
@EqualsAndHashCode(callSuper = true)
public class SubmitChangeDTO extends BaseSubmitDTO {

	@Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "\"1320098173412546\"")
	private String taskId;

	@Schema(description = "UPSCALE(放大); VARIATION(变换); REROLL(重新生成)", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"UPSCALE", "VARIATION", "REROLL"}, example = "UPSCALE")
	private TaskAction action;

	@Schema(description = "序号(1~4), action为UPSCALE,VARIATION时必传", minimum = "1", maximum = "4", example = "1")
	private Integer index;

}
