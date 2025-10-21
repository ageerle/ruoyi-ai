package org.ruoyi.workflow.base;

import lombok.Data;
import org.ruoyi.workflow.enums.ErrorEnum;

import java.io.Serializable;

@Data
public class BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 状态码
     */
    private String code;
    /**
     * 提示
     */
    private String message;
    /**
     * 数据
     */
    private T data;

    public BaseResponse() {
    }

    public BaseResponse(boolean success) {
        this.success = success;
    }

    public BaseResponse(boolean success, T data) {
        this.data = data;
        this.success = success;
    }

    public BaseResponse(String code, String message, T data) {
        this.code = code;
        this.success = false;
        this.message = message;
        this.data = data;
    }

    public static BaseResponse success(String message) {
        return new BaseResponse(ErrorEnum.SUCCESS.getCode(), message, "");
    }
}
