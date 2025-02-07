package org.ruoyi.common.chat.openai.exception;

/**
 * 描述： 错误
 *
 * @author https:www.unfbx.com
 *  2023-02-11
 */
public enum CommonError implements IError {
    API_KEYS_NOT_NUL(500, "API KEYS 不能为空"),
    NO_ACTIVE_API_KEYS(500, "没有可用的API KEYS"),
    SYS_ERROR(500, "系统繁忙"),
    PARAM_ERROR(501, "参数异常"),
    RETRY_ERROR(502, "请求异常，请重试~"),
    //官方的错误码列表：https://platform.openai.com/docs/guides/error-codes/api-errors
    OPENAI_AUTHENTICATION_ERROR(401, "身份验证无效/提供的 API 密钥不正确/您必须是组织的成员才能使用 API"),
    OPENAI_LIMIT_ERROR(429 , "达到请求的速率限制/您超出了当前配额，请检查您的计划和帐单详细信息/发动机当前过载，请稍后重试"),
    OPENAI_SERVER_ERROR(500, "服务器在处理您的请求时出错"),
    ;


    private int code;
    private String msg;

    CommonError(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String msg() {
        return this.msg;
    }

    @Override
    public int code() {
        return this.code;
    }
}
