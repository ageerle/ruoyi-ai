package org.ruoyi.chat.domain.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "任务查询参数")
public class TaskConditionDTO {

	@ArraySchema(arraySchema = @Schema(description = "任务ID列表"), schema = @Schema(example = "1320098173412546"))
	private List<String> ids;

}
