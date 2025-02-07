package org.ruoyi.system.domain.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author https://www.wdbyte.com
 */
@Slf4j
@Getter
@Setter
public class ApiResult {
    private Integer code;
    private String message;
    private Object data;

    public ApiResult() {
    }

    public ApiResult(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public ApiResult(Integer code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
