package com.xmzs.common.chat.domain.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * mj任务请求实体类
 *
 * @author WangLe
 */
@Data
public class MjTaskRequest {

    private String prompt;
}
