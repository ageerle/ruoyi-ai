package org.ruoyi.chat.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(name = "变化任务提交参数")
public class SubmitActionDTO {

    private String customId;

    private String taskId;

    private String state;

    private String notifyHook;
}
