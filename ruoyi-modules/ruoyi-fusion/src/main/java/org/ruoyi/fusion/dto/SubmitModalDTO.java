package org.ruoyi.fusion.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("局部重绘提交参数")
public class SubmitModalDTO extends BaseSubmitDTO{

	private String maskBase64;

	private String taskId;

    private String prompt;

}
