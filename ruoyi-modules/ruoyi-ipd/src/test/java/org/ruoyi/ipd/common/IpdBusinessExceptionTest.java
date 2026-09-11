package org.ruoyi.ipd.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.advice.IpdServiceExceptionAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API-01 验收：IpdBusinessException 携带 ApiV1ErrorCode 时，IpdServiceExceptionAdvice
 * 必须返回对应 HTTP 状态 + ApiV1Response(code=errorCode.code, message=exception.message)。
 */
@Tag("dev")
class IpdBusinessExceptionTest {

    private final IpdServiceExceptionAdvice advice = new IpdServiceExceptionAdvice();

    @Test
    @DisplayName("STATE_CONFLICT 业务异常 → 409 + code=50002 + message=错误码默认文案")
    void stateConflictMapped() {
        ResponseEntity<ApiV1Response<Void>> r = advice.handleIpdBusiness(
            new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().getCode()).isEqualTo(ApiV1ErrorCode.STATE_CONFLICT.getCode());
        assertThat(r.getBody().getMessage()).isEqualTo(ApiV1ErrorCode.STATE_CONFLICT.getMessage());
    }

    @Test
    @DisplayName("GATE_NOT_PASSED 业务异常 → 409 + code=40001")
    void gateNotPassedMapped() {
        ResponseEntity<ApiV1Response<Void>> r = advice.handleIpdBusiness(
            new IpdBusinessException(ApiV1ErrorCode.GATE_NOT_PASSED));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().getCode()).isEqualTo(ApiV1ErrorCode.GATE_NOT_PASSED.getCode());
    }

    @Test
    @DisplayName("ROLE_LOCKED 业务异常 → 409 + code=40004")
    void roleLockedMapped() {
        ResponseEntity<ApiV1Response<Void>> r = advice.handleIpdBusiness(
            new IpdBusinessException(ApiV1ErrorCode.ROLE_LOCKED));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().getCode()).isEqualTo(ApiV1ErrorCode.ROLE_LOCKED.getCode());
    }

    @Test
    @DisplayName("String 构造（自定义文案）→ PARAM_INVALID/400 + 自定义 message")
    void stringConstructorMapsParamInvalid() {
        ResponseEntity<ApiV1Response<Void>> r = advice.handleIpdBusiness(
            new IpdBusinessException("项目已结项，无法再绑定产品"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().getCode()).isEqualTo(ApiV1ErrorCode.PARAM_INVALID.getCode());
        assertThat(r.getBody().getMessage()).contains("项目已结项");
    }

    @Test
    @DisplayName("NOT_FOUND 业务异常 → 404 + code=50001")
    void notFoundMapped() {
        ResponseEntity<ApiV1Response<Void>> r = advice.handleIpdBusiness(
            new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody().getCode()).isEqualTo(ApiV1ErrorCode.NOT_FOUND.getCode());
    }
}
