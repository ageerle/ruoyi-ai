package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.ruoyi.ipd.service.AuditLogService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P1-6 超管要素管理：编码唯一/门禁枚举校验/停用禁删/审计落链。
 */
@Tag("dev")
class GateElementServiceTest {

    private GateElementMapper mapper;
    private AuditLogService auditLogService;
    private GateElementService service;

    @BeforeEach
    void setUp() {
        mapper = mock(GateElementMapper.class);
        auditLogService = mock(AuditLogService.class);
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new GateElementService(mapper, auditLogService);
    }

    private GateElement e(String code) {
        return GateElement.builder().gateCode("G1").elementCode(code).elementName("市场吸引力")
            .isVeto("0").sortOrder(1).build();
    }

    @Test
    @DisplayName("创建合法要素：默认 enabled=1 且写审计")
    void createOk() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(mapper.insert(any(GateElement.class))).thenAnswer(inv -> {
            GateElement x = inv.getArgument(0);
            x.setId(9L);
            return 1;
        });
        GateElement out = service.create(e("G1-01"), "admin");
        assertThat(out.getEnabled()).isEqualTo("1");
        Mockito.verify(auditLogService).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("要素编码重复拒绝")
    void duplicateCodeRejected() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThatThrownBy(() -> service.create(e("G1-01"), "admin"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已存在");
    }

    @Test
    @DisplayName("gateCode 白名单 G1..G5，越界拒绝")
    void gateWhitelist() {
        assertThatThrownBy(() -> service.create(GateElement.builder()
                .gateCode("G9").elementCode("X").elementName("x").build(), "admin"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("G1..G5");
    }

    @Test
    @DisplayName("update 部分字段：仅 patch 非空字段生效")
    void updatePartial() {
        GateElement exist = e("G1-01");
        exist.setId(1L);
        exist.setPassStandard("原标准");
        when(mapper.selectById(1L)).thenReturn(exist);
        GateElement patch = GateElement.builder().elementName("新名").build();
        patch.setId(1L);
        GateElement out = service.update(patch, "admin");
        assertThat(out.getElementName()).isEqualTo("新名");
        assertThat(out.getPassStandard()).isEqualTo("原标准");
    }

    @Test
    @DisplayName("停用禁删：enabled=0 且审计留痕")
    void disableNotDelete() {
        GateElement exist = e("G1-01");
        exist.setId(2L);
        exist.setEnabled("1");
        when(mapper.selectById(2L)).thenReturn(exist);
        GateElement out = service.disable(2L, "admin");
        assertThat(out.getEnabled()).isEqualTo("0");
        Mockito.verify(auditLogService).append(any(AuditLog.class));
    }
}