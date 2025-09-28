package org.ruoyi.chat.domain.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.chat.enums.BlendDimensions;

import java.util.List;

@Data
@Schema(name = "Blend提交参数")
@EqualsAndHashCode(callSuper = true)
public class SubmitBlendDTO extends BaseSubmitDTO {

	@ArraySchema(arraySchema = @Schema(description = "图片base64数组", requiredMode = Schema.RequiredMode.REQUIRED), schema = @Schema(example = "data:image/png;base64,xxx1"))
	private List<String> base64Array;

	@Schema(description = "比例: PORTRAIT(2:3); SQUARE(1:1); LANDSCAPE(3:2)", example = "SQUARE")
	private BlendDimensions dimensions = BlendDimensions.SQUARE;
}
