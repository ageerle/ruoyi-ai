package org.ruoyi.common.wechat.web.base;

/**
 * @author WesleyOne
 * @create 2018/7/28
 */
public class BaseResponse<T> {
    public static BaseResponse OK = new BaseResponse();

    private String code = "00";
    private String message = "操作成功";

    private T data;
    public BaseResponse() {
    }

    public BaseResponse(T data) {
        this.data = data;
    }

    public BaseResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static BaseResponse success(){
        return new BaseResponse();
    }

    public static BaseResponse success(Object o){
        return new BaseResponse(o);
    }

    public static BaseResponse error(String code,String msg){
        BaseResponse r = new BaseResponse();
        r.setCode(code);
        r.setMessage(msg);
        return r;
    }
    public static BaseResponse error(BaseError baseError){
        BaseResponse r = new BaseResponse();
        r.setCode(baseError.getCode());
        r.setMessage(baseError.getMsg());
        return r;
    }

    /**
     * 未登录返回
     * @return
     */
    public static BaseResponse unLogin(){
        BaseResponse r = new BaseResponse();
        r.setCode(BaseError.UNLOGIN.getCode());
        r.setMessage(BaseError.UNLOGIN.getMsg());
        return r;
    }

    /**
     * 无权限返回
     * @return
     */
    public static BaseResponse unPermission(){
        BaseResponse r = new BaseResponse();
        r.setCode(BaseError.UNPERMISSION.getCode());
        r.setMessage(BaseError.UNPERMISSION.getMsg());
        return r;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
