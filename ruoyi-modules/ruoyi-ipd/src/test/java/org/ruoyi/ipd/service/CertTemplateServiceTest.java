package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.CertTemplate;
import org.ruoyi.ipd.mapper.CertTemplateMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证模板库单测：resolve 带出/去重/必填/审计
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class CertTemplateServiceTest {

    @Mock
    private CertTemplateMapper mapper;
    @Mock
    private AuditLogService auditLogService;

    private CertTemplateService service;

    @BeforeEach
    void setUp() {
        service = new CertTemplateService(mapper, auditLogService);
    }

    private CertTemplate tpl(String cc, String cn, String name, String mandatory) {
        CertTemplate t = new CertTemplate();
        t.setId(1L);
        t.setCountryCode(cc);
        t.setCountryName(cn);
        t.setCertName(name);
        t.setIsMandatory(mandatory);
        t.setDelFlag("0");
        return t;
    }

    @Test
    @DisplayName("resolve：多目标市场带出（SA 沙特 + AE 阿联酋）")
    void resolveMarkets() {
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            tpl("SA", "沙特阿拉伯", "SABER/SASO", "1"),
            tpl("AE", "阿联酋", "ECAS/EQM", "1")));
        List<CertTemplate> resolved = service.resolve(new String[]{"SA", "AE"});
        assertThat(resolved).hasSize(2);
        assertThat(resolved).extracting(CertTemplate::getCertName)
            .containsExactly("SABER/SASO", "ECAS/EQM");
    }

    @Test
    @DisplayName("resolve：空市场/空白串拒绝")
    void resolveEmpty() {
        assertThatThrownBy(() -> service.resolve(null))
            .isInstanceOf(ServiceException.class).hasMessageContaining("目标市场为空");
        assertThatThrownBy(() -> service.resolve(new String[]{"  "}))
            .isInstanceOf(ServiceException.class).hasMessageContaining("目标市场为空");
    }

    @Test
    @DisplayName("create：三字段必填 + 同国同名去重")
    void createValidation() {
        assertThatThrownBy(() -> service.create(tpl("", "中国", "CCC", "1"), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("必填");

        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThatThrownBy(() -> service.create(tpl("CN", "中国", "CCC", "1"), 1L))
            .isInstanceOf(ServiceException.class).hasMessageContaining("同名认证项");
    }

    @Test
    @DisplayName("create 成功 + 审计；remove 禁止直删旁路（P0-6.2）")
    void createAndRemove() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        CertTemplate created = service.create(tpl("CN", "中国", "SRRC", "1"), 1L);
        assertThat(created.getIsMandatory()).isEqualTo("1");
        verify(auditLogService).append(any());

        assertThatThrownBy(() -> service.remove(1L, 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("禁止直删");
    }
}