package com.xmzs.midjourney.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;


@Data
@ApiModel("变化任务提交参数")
public class SubmitActionDTO {

	private String customId;

	private String taskId;

    private String state;

    private String notifyHook;
}
