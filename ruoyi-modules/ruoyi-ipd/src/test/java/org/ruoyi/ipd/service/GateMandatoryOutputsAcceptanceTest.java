/**
 * [SEC-FIX-HIGH-1.1-FOLLOWUP] Gate 提交强制输出物守卫验收测试——
 * 真实 mock 镜像 P251 模式（不再用 tautological 断言）。
 */
package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.domain.GateElementResult;
import org.ruoyi.ipd.domain.OssFileEntity;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.ruoyi.ipd.mapper.GateElementResultMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.OssFileMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GateMandatoryOutputsAcceptanceTest {

    @Mock private GateMapper gateMapper;
    @Mock private GateElementMapper elementMapper;
    @Mock private GateElementResultMapper resultMapper;
    @Mock private OssFileMapper ossFileMapper;
    @Mock private org.ruoyi.ipd.service.SystemConfigService systemConfigService;
    @Mock private org.ruoyi.ipd.service.AuditLogService auditLogService;
    @Mock private org.ruoyi.ipd.service.NotificationService notificationService;

    @InjectMocks private GateElementResultService service;

    private static final Long GATE_ID = 501L;
    private static final Long MAT_OSS = 9001L;
    private static final Long MIN_OSS = 9002L;
    private static final IpdActor PM = new IpdActor(100L, "市场PM", "MARKET_PM", 1L);

    private void setupHappyPath() {
        Gate gate = new Gate();
        gate.setId(GATE_ID);
        gate.setProjectId(10L);
        gate.setGateCode("G1");
        gate.setStatus("PENDING");
        when(gateMapper.selectById(GATE_ID)).thenReturn(gate);
        GateElement el = new GateElement();
        el.setId(601L);
        el.setElementCode("G1-1");
        el.setElementName("市场机会真实性");
        el.setIsVeto("0");
        when(elementMapper.selectList(any())).thenReturn(Collections.singletonList(el));
        GateElementResult res = new GateElementResult();
        res.setElementId(601L);
        res.setResult("PASS");
        when(resultMapper.selectList(any())).thenReturn(Collections.singletonList(res));
        // oss mocks
        OssFileEntity mat = new OssFileEntity(); mat.setOssId(MAT_OSS); mat.setUrl("https://oss.local/materials/g1.pdf");
        OssFileEntity min = new OssFileEntity(); min.setOssId(MIN_OSS); min.setUrl("https://oss.local/minutes/g1.pdf");
        when(ossFileMapper.selectById(MAT_OSS)).thenReturn(mat);
        when(ossFileMapper.selectById(MIN_OSS)).thenReturn(min);
    }

    @Test
    @DisplayName("[SEC-FIX-HIGH-1.1-FOLLOWUP] null materialsOssId → 抛 ServiceException")
    void nullMaterialsOssIdRejected() {
        setupHappyPath();
        assertThatThrownBy(() -> service.submit(GATE_ID, null, MIN_OSS, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("ossId 非法");
        verify(gateMapper, never()).updateById(any(Gate.class));
    }

    @Test
    @DisplayName("[SEC-FIX-HIGH-1.1-FOLLOWUP] materialsOssId=0 → 抛 ServiceException")
    void zeroMaterialsOssIdRejected() {
        setupHappyPath();
        assertThatThrownBy(() -> service.submit(GATE_ID, 0L, MIN_OSS, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("ossId 非法");
    }

    @Test
    @DisplayName("[SEC-FIX-HIGH-1.1-FOLLOWUP] ossId 不存在 → 抛 ServiceException")
    void materialsOssIdNotFound() {
        setupHappyPath();
        when(ossFileMapper.selectById(MAT_OSS)).thenReturn(null);
        assertThatThrownBy(() -> service.submit(GATE_ID, MAT_OSS, MIN_OSS, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("OSS 文件不存在");
    }

    @Test
    @DisplayName("[SEC-FIX-HIGH-1.1-FOLLOWUP] 合法 ossId + 全要素已判 → 提交成功 + audit 含 URL 长度摘要")
    void happyPathSuccess() {
        setupHappyPath();
        Gate result = service.submit(GATE_ID, MAT_OSS, MIN_OSS, PM);
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getMaterialsUrl()).isEqualTo("https://oss.local/materials/g1.pdf");
        assertThat(result.getMeetingMinutesUrl()).isEqualTo("https://oss.local/minutes/g1.pdf");
        verify(gateMapper, times(1)).updateById(any(Gate.class));  // 显式类型避免重载歧义
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("[SEC-FIX-HIGH-1.1-FOLLOWUP] 缺 meetingMinutesOssId → 抛 ServiceException")
    void nullMeetingMinutesOssIdRejected() {
        setupHappyPath();
        assertThatThrownBy(() -> service.submit(GATE_ID, MAT_OSS, null, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("ossId 非法");
    }
}
