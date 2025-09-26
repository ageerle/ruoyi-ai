package org.ruoyi.chat.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseSubmitDTO {

	@Schema(description = "自定义参数")
	protected String state;

	@Schema(description = "回调地址, 为空时使用全局notifyHook")
	protected String notifyHook;
}
