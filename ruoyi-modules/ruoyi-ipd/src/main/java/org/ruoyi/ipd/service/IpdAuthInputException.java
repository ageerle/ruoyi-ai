package org.ruoyi.ipd.service;

/** 已认证用户的可纠正输入错误；消息仅来自固定枚举，不携带凭据。 */
public final class IpdAuthInputException extends RuntimeException {
    public enum Reason {
        PASSWORD_LENGTH("新密码长度至少 8 位，UTF-8编码不得超过72字节"),
        CURRENT_PASSWORD_INCORRECT("原密码错误"),
        PASSWORD_UNCHANGED("新密码不能与当前密码相同");

        private final String message;
        Reason(String message) { this.message = message; }
    }

    private final Reason reason;

    public IpdAuthInputException(Reason reason) {
        super(reason.message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
