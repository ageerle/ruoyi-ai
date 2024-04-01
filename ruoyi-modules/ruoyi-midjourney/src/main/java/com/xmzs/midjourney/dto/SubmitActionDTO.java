package com.xmzs.midjourney.dto;

import com.xmzs.midjourney.enums.TaskAction;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@ApiModel("变化任务提交参数")
public class SubmitActionDTO {

	private String customId;

	private String taskId;

    private String state;

    private String notifyHook;
}
