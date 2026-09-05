package org.ruoyi.ipd.security;

import lombok.Getter;
import org.ruoyi.ipd.common.ApiV1ErrorCode;

/** 仅用于IPD身份/角色拒绝；不泄漏对象名称或人员详情。 */
@Getter
public class IpdPermissionException extends RuntimeException {
    private final int httpStatus;
    private final ApiV1ErrorCode errorCode;

    public IpdPermissionException(int httpStatus, ApiV1ErrorCode errorCode) {
        super(errorCode.name());
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
