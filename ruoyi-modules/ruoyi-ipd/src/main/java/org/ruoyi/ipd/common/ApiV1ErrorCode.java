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
    PRODUCT_INACTIVE(40401, "产品已下架"),
    RATE_LIMITED(40011, "请求过于频繁，请稍后重试"),
    ATTACHMENT_TOO_LARGE(40012, "附件数量或大小超出限制"),
    AI_BUDGET_EXCEEDED(40013, "AI预算超出限制"),

    NOT_FOUND(50001, "资源不存在"),
    STATE_CONFLICT(50002, "状态冲突"),

    /** P3-1.1/1.2/1.3：KPI 周期格式错（应为 YYYY-MM） */
    KPI_PERIOD_INVALID(50003, "KPI 周期格式应为 YYYY-MM"),
    /** P3-1.3：KPI 趋势回看期数越界（1~36） */
    KPI_PERIOD_RANGE_INVALID(50004, "KPI 趋势期数必须在 1~36 区间"),
    /** P3-6.2：贡献度比例超区间（市场 40%-65%，研发 35%-60%） */
    CONTRIB_TIER_OUT_OF_RANGE(50005, "贡献度比例超区间（市场 PM 必须在 40%-65%，研发 PM 必须在 35%-60%）"),
    /** P3-6.2：五维度权重和不等于 100% */
    CONTRIB_DIM_SUM_NOT_100(50006, "贡献度五维度权重之和必须等于 100%"),
    /** P3-6.2：贡献度评定入口仅在 G5 上市后 90 天复盘阶段开放 */
    CONTRIB_NOT_G5_STAGE(50007, "贡献度评定入口仅在 G5 上市后 90 天复盘阶段开放"),
    /** P3-6.2：贡献度评定权限不足（仅双 PM 自评 + 各自产品组长） */
    CONTRIB_NOT_AUTHORIZED(50008, "贡献度评定权限不足（仅双 PM 自评 + 各自产品组长）"),
    /** P3-8.2：负反馈触发情形非法（仅 REWORK_EXCEEDED|QUALITY_ACCIDENT|SPEC_PILE_COPY|MISSED_MARKET_WINDOW） */
    NF_TRIGGER_TYPE_INVALID(50009, "负反馈触发情形非法（仅 REWORK_EXCEEDED|QUALITY_ACCIDENT|SPEC_PILE_COPY|MISSED_MARKET_WINDOW）"),
    /** P3-8.2：月份格式错（应为 YYYY-MM） */
    NF_MONTH_FORMAT_INVALID(50010, "月份格式错（应为 YYYY-MM）"),
    /** P3-8.2：项目无 MARKET_PM / RD_PM 成员，无法执行负反馈 */
    NF_NOT_PM(50011, "项目无 MARKET_PM / RD_PM 成员，无法执行负反馈"),
    /** P3-8.2：同项目同 triggerType 已存在，AC-INC-40 重复事件不重复扣减 */
    NF_REENTRY_NOT_ALLOWED(50012, "同项目同触发情形已存在负反馈记录（AC-INC-40 重复事件不重复扣减）"),
    /** P3-8.2：状态机不允许此操作（仅 DRAFT 可 submit；仅 PENDING_DECISION 可 decide；仅 EXECUTED 可 lift） */
    NF_STATE_INVALID(50013, "负反馈状态机不允许此操作"),

    /** P3-7.1：已锁定的月份上写入账务记录 */
    SWITCHING_LOCKED(50014, "该月份已锁定，不允许写入账务记录（SWITCHING_LOCKED）"),
    /** P3-7.1：对账差异率 ≥ 1% 时尝试 lock */
    SWITCHING_DIFF_TOO_LARGE(50015, "对账差异率 ≥ 1%，不允许锁定（SWITCHING_DIFF_TOO_LARGE）"),
    /** P3-7.1：lock 调用时未先 run */
    SWITCHING_NOT_RUN(50016, "该月份尚未运行对账（SWITCHING_NOT_RUN）"),

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
            case PARAM_INVALID, KPI_PERIOD_INVALID, KPI_PERIOD_RANGE_INVALID,
                CONTRIB_TIER_OUT_OF_RANGE, CONTRIB_DIM_SUM_NOT_100,
                NF_TRIGGER_TYPE_INVALID, NF_MONTH_FORMAT_INVALID -> 400;
            case UNAUTHORIZED -> 401;
            case ACCOUNT_FROZEN_PENDING_HANDOVER, ACCOUNT_PASSWORD_CHANGE_REQUIRED, FORBIDDEN,
                CONTRIB_NOT_AUTHORIZED, NF_NOT_PM -> 403;
            case GATE_NOT_PASSED, DUAL_SIGN_INCOMPLETE, OVER_QUOTA_NOT_REGISTERED, ROLE_LOCKED,
                DELETE_NOT_ALLOWED_DIRECT, HANDOVER_REQUIRED_BEFORE_DISABLE, STATE_CONFLICT,
                AI_BUDGET_EXCEEDED, CONTRIB_NOT_G5_STAGE, NF_REENTRY_NOT_ALLOWED, NF_STATE_INVALID,
                SWITCHING_LOCKED, SWITCHING_DIFF_TOO_LARGE, SWITCHING_NOT_RUN -> 409;
            case RATE_LIMITED -> 429;
            case ATTACHMENT_TOO_LARGE -> 413;
            case PRODUCT_INACTIVE, NOT_FOUND -> 404;
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
