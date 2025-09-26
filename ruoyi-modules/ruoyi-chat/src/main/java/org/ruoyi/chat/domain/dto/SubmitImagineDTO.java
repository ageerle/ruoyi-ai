package org.ruoyi.chat.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Data
@Schema(name = "Imagine提交参数")
@EqualsAndHashCode(callSuper = true)
public class SubmitImagineDTO extends BaseSubmitDTO {

	@Schema(description = "提示词", requiredMode = Schema.RequiredMode.REQUIRED, example = "Cat")
	private String prompt;

	@Schema(description = "垫图base64数组")
	private List<String> base64Array;

	@Schema(hidden = true)
	@Deprecated(since = "3.0", forRemoval = true)
	private String base64;

}
