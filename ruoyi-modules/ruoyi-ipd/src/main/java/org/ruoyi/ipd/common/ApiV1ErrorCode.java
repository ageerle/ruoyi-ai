package org.ruoyi.ipd.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * IPD 统一错误码（v3 TS-09：0 成功；1xxxx 参数 / 2xxxx 认证 / 3xxxx 权限 /
 * 4xxxx 业务规则阻断 / 5xxxx 资源不存在或状态冲突 / 9xxxx 系统内部）
 * 具体码值按分期实现逐步登记；新码必须落在对应段位内。
 */
@Getter
@AllArgsConstructor
public enum ApiV1ErrorCode {

    OK(0, "ok"),

    // —— 1xxxx 参数校验错误 ——
    PARAM_INVALID(10001, "参数校验失败"),

    // —— 2xxxx 认证/鉴权错误 ——
    UNAUTHORIZED(20001, "未认证或凭证失效"),
    ACCOUNT_FROZEN_PENDING_HANDOVER(20002, "账号待移交冻结中，仅保留移交相关权限"),

    // —— 3xxxx 权限不足 ——
    FORBIDDEN(30001, "权限不足"),

    // —— 4xxxx 业务规则阻断（门禁/双签/超项等） ——
    GATE_NOT_PASSED(40001, "阶段门禁未通过"),
    DUAL_SIGN_INCOMPLETE(40002, "双签未完成"),
    OVER_QUOTA_NOT_REGISTERED(40003, "超项未备案"),
    ROLE_LOCKED(40004, "角色固定不可跨（市场PM/研发PM）"),
    DELETE_NOT_ALLOWED_DIRECT(40005, "禁止直接删除，须走两级删除审核"),
    HANDOVER_REQUIRED_BEFORE_DISABLE(40006, "先完成移交才可禁用账号"),

    // —— 5xxxx 资源不存在/状态冲突 ——
    NOT_FOUND(50001, "资源不存在"),
    STATE_CONFLICT(50002, "状态冲突"),

    // —— 9xxxx 系统内部错误 ——
    INTERNAL_ERROR(90001, "系统内部错误");

    private final int code;
    private final String message;
}