package com.xmzs.common.chat.domain.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @sine 2023-04-08
 */
@Data
public class SaveMsgRequest {

    @NotEmpty(message = "传入的模型不能为空")
    private String model;

    @NotEmpty(message = "对话消息不能为空")
    private String msg;

}
