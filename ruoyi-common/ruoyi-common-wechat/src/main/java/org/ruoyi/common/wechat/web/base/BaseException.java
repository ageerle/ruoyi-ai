package org.ruoyi.common.wechat.web.base;

import java.io.Serializable;

/**
 * 统一异常对象
 * @author WesleyOne
 * @create 2018/7/28
 */
public class BaseException extends Exception implements Serializable {

    private static final long serialVersionUID = 2007525058641283836L;

    private String code;

    public BaseException(String code, String msg) {
        super(msg);
        this.code = code;

    }

    public BaseException(BaseError baseError) {
        super(baseError.getMsg());
        this.code = baseError.getCode();
    }

    public BaseException(String msg) {
        super(msg);
        this.code = BaseError.NORMAL_ERR.getCode();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
