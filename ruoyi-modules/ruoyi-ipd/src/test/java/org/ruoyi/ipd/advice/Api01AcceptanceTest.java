package org.ruoyi.ipd.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API-01 验收测试：IpdServiceExceptionAdvice 把基线 R 包络统一迁至 ApiV1Response。
 * 用直接调用 handler 方法（不需 @SpringBootTest，验证逻辑正确性 + HTTP 状态码映射）。
 */
@Tag("dev")
class Api01AcceptanceTest {

    private final IpdServiceExceptionAdvice advice = new IpdServiceExceptionAdvice();

    @Test
    @DisplayName("ServiceException with explicit code → ApiV1Response 包络 + 对应 HTTP 状态")
    void serviceExceptionWithCode() {
        ResponseEntity<ApiV1Response<Void>> r = advice.handleServiceException(
            new ServiceException("x", ApiV1ErrorCode.STATE_CONFLICT.getCode()));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody()).isNotNull();
        assertThat(r.getBody().getCode()).isEqualTo(ApiV1ErrorCode.STATE_CONFLICT.getCode());
    }

    @Test
    @DisplayName("ServiceException 无 code → 降级为 PARAM_INVALID（400）")
    void serviceExceptionWithoutCode() {
        ResponseEntity<ApiV1Response<Void>> r = advice.handleServiceException(
            new ServiceException("countryCode/countryName/certName 必填"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().getCode()).isEqualTo(ApiV1ErrorCode.PARAM_INVALID.getCode());
        assertThat(r.getBody().getMessage()).contains("countryCode");
    }

    @Test
    @DisplayName("ServiceException with unknown code → INTERNAL_ERROR（90001/500）")
    void serviceExceptionWithUnknownCode() {
        ResponseEntity<ApiV1Response<Void>> r = advice.handleServiceException(
            new ServiceException("x", 99999));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.getBody().getCode()).isEqualTo(ApiV1ErrorCode.INTERNAL_ERROR.getCode());
    }

    @Test
    @DisplayName("Exception 兜底 → 500 + INTERNAL_ERROR（不泄漏 e.getMessage）")
    void exceptionFallback() {
        ResponseEntity<ApiV1Response<Void>> r = advice.handleUnexpected(
            new RuntimeException("database password leaked"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.getBody().getCode()).isEqualTo(ApiV1ErrorCode.INTERNAL_ERROR.getCode());
        // 关键安全属性：不得把底层异常 message 暴露给客户端
        assertThat(r.getBody().getMessage()).doesNotContain("database password");
    }
}
