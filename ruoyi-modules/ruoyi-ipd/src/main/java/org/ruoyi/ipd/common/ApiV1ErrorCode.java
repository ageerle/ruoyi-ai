package org.ruoyi.ipd.common;

/** IPD 统一错误码；HTTP 映射以 DOC-06 §2.2 为准，不按数字段或异常消息猜测。 */
public enum ApiV1ErrorCode {
    OK(0, "ok"),

    PARAM_INVALID(10001, "参数校验失败"),

    UNAUTHORIZED(20001, "未认证或凭证失效"),
    ACCOUNT_FROZEN_PENDING_HANDOVER(20002, "账号待移交冻结中，仅保留移交相关权限"),
    ACCOUNT_PASSWORD_CHANGE_REQUIRED(20003, "首登强制改密：仅允许调用改密相关接口"),

    FORBIDDEN(30001, "权限不足"),

    GATE_NOT_PASSED(40001, "阶段门禁未通过"),
    DUAL_SIGN_INCOMPLETE(40002, "双签未完成"),
    OVER_QUOTA_NOT_REGISTERED(40003, "超项未备案"),
    ROLE_LOCKED(40004, "角色固定不可跨（市场PM/研发PM）"),
    DELETE_NOT_ALLOWED_DIRECT(40005, "禁止直接删除，须按数据分级完成删除审核"),
    HANDOVER_REQUIRED_BEFORE_DISABLE(40006, "先完成移交才可禁用账号"),
    RATE_LIMITED(40011, "请求过于频繁，请稍后重试"),
    ATTACHMENT_TOO_LARGE(40012, "附件数量或大小超出限制"),
    AI_BUDGET_EXCEEDED(40013, "AI预算超出限制"),

    NOT_FOUND(50001, "资源不存在"),
    STATE_CONFLICT(50002, "状态冲突"),

    INTERNAL_ERROR(90001, "系统内部错误");

    private final int code;
    private final String message;

    /**
     * @param code    业务码
     * @param message 默认文案
     */
    ApiV1ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return switch (this) {
            case OK -> 200;
            case PARAM_INVALID -> 400;
            case UNAUTHORIZED -> 401;
            case ACCOUNT_FROZEN_PENDING_HANDOVER, ACCOUNT_PASSWORD_CHANGE_REQUIRED, FORBIDDEN -> 403;
            case GATE_NOT_PASSED, DUAL_SIGN_INCOMPLETE, OVER_QUOTA_NOT_REGISTERED, ROLE_LOCKED,
                DELETE_NOT_ALLOWED_DIRECT, HANDOVER_REQUIRED_BEFORE_DISABLE, STATE_CONFLICT,
                AI_BUDGET_EXCEEDED -> 409;
            case RATE_LIMITED -> 429;
            case ATTACHMENT_TOO_LARGE -> 413;
            case NOT_FOUND -> 404;
            case INTERNAL_ERROR -> 500;
        };
    }

    /** 只解析已登记的失败码；旧无类型异常和误传成功码不得伪装成业务拒绝。 */
    public static ApiV1ErrorCode fromCode(Integer code) {
        if (code != null && code != OK.code) {
            for (ApiV1ErrorCode value : values()) {
                if (value.code == code) return value;
            }
        }
        return INTERNAL_ERROR;
    }
}
