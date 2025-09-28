package org.ruoyi.chat.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "局部重绘提交参数")
public class SubmitModalDTO extends BaseSubmitDTO{

	private String maskBase64;

	private String taskId;

    private String prompt;

}
