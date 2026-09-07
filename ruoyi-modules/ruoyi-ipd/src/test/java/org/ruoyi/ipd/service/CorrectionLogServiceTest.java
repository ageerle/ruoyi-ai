package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.CorrectionLog;
import org.ruoyi.ipd.mapper.CorrectionLogMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P3-2.2 CorrectionLogService 单测（5 测覆盖 4 维度）：
 * <ol>
 *   <li>正常路径：record() 写入 CorrectionLog（含 operatorId/name/operatedAt/audit bindCreateAudit）</li>
 *   <li>边界：oldValue 与 newValue 相同时 NO-OP（不写库）</li>
 *   <li>异常：必填字段缺失（actor/entityType/entityId/fieldName/newValue/reason）抛 PARAM_INVALID</li>
 *   <li>读取：listByEntity 仅超管；按 operatedAt DESC 透传 selectList</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class CorrectionLogServiceTest {

    @Mock private CorrectionLogMapper correctionLogMapper;
    @Mock private IpdPermission permission;

    private CorrectionLogService service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p322-correction-test"),
            CorrectionLog.class);
    }

    @BeforeEach
    void setUp() {
        service = new CorrectionLogService(correctionLogMapper, permission);
    }

    private IpdActor actor() {
        return new IpdActor(8001L, "operator-1", "GROUP_LEADER", 20L);
    }

    @Test
    @DisplayName("正常：record() 写入一行 CorrectionLog，字段全部透传")
    void record_writesRowWithFullFields() {
        // actor 入参可来自 requireInternal()，但本 service.record() 不强制 requireAdmin
        // （写入留痕仅校验 actor != null 与必填字段），避免不必要的 stubbing。

        service.record(actor(), "ProjectScore", 101L, "weightedScore",
            "85.50", "88.00", "终态更正");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<CorrectionLog> cap = ArgumentCaptor.forClass(CorrectionLog.class);
        verify(correctionLogMapper, times(1)).insert(cap.capture());
        CorrectionLog row = cap.getValue();
        assertThat(row.getEntityType()).isEqualTo("ProjectScore");
        assertThat(row.getEntityId()).isEqualTo(101L);
        assertThat(row.getFieldName()).isEqualTo("weightedScore");
        assertThat(row.getOldValue()).isEqualTo("85.50");
        assertThat(row.getNewValue()).isEqualTo("88.00");
        assertThat(row.getReason()).isEqualTo("终态更正");
        assertThat(row.getOperatorId()).isEqualTo(8001L);
        assertThat(row.getOperatorName()).isEqualTo("operator-1");
        assertThat(row.getOperatedAt()).isNotNull();
    }

    @Test
    @DisplayName("边界：oldValue 与 newValue 相同视为 NO-OP，不入库")
    void record_sameValueShortCircuits() {
        service.record(actor(), "ProjectScore", 101L, "weightedScore",
            "85.50", "85.50", "no-op");

        verify(correctionLogMapper, never()).insert(any(CorrectionLog.class));
    }

    @Test
    @DisplayName("异常：必填字段缺失一律抛 PARAM_INVALID，不入库")
    void record_requiredFieldsValidated() {
        IpdActor a = actor();

        // actor null
        assertThatThrownBy(() -> service.record(null, "ProjectScore", 1L, "f", "o", "n", "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);

        // entityType blank
        assertThatThrownBy(() -> service.record(a, "  ", 1L, "f", "o", "n", "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // entityId null
        assertThatThrownBy(() -> service.record(a, "ProjectScore", null, "f", "o", "n", "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // fieldName blank
        assertThatThrownBy(() -> service.record(a, "ProjectScore", 1L, "", "o", "n", "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // newValue null
        assertThatThrownBy(() -> service.record(a, "ProjectScore", 1L, "f", "o", null, "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // reason blank
        assertThatThrownBy(() -> service.record(a, "ProjectScore", 1L, "f", "o", "n", "  "))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        verify(correctionLogMapper, never()).insert(any(CorrectionLog.class));
    }

    @Test
    @DisplayName("读取：listByEntity 仅超管；透传 selectList（operatedAt DESC）")
    void listByEntity_adminOnlyAndPassesThrough() {
        when(permission.requireAdmin()).thenReturn(new IpdActor(9001L, "admin", "SUPER_ADMIN", 0L));
        CorrectionLog r1 = CorrectionLog.builder().id(2L).entityType("ProjectScore").entityId(101L)
            .fieldName("weightedScore").oldValue("o").newValue("n").reason("r")
            .operatorId(8001L).operatorName("op").operatedAt(new java.util.Date()).build();
        CorrectionLog r2 = CorrectionLog.builder().id(1L).entityType("ProjectScore").entityId(101L)
            .fieldName("weightedScore").oldValue("o2").newValue("n2").reason("r2")
            .operatorId(8002L).operatorName("op2").operatedAt(new java.util.Date(0L)).build();
        when(correctionLogMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(r1, r2));

        List<CorrectionLog> result = service.listByEntity("ProjectScore", 101L);

        assertThat(result).hasSize(2);
        verify(permission, times(1)).requireAdmin();
        verify(correctionLogMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("权限：listByEntity 非超管抛 403；不触达 mapper")
    void listByEntity_adminOnly() {
        when(permission.requireAdmin())
            .thenThrow(new IpdPermissionException(403, ApiV1ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> service.listByEntity("ProjectScore", 101L))
            .isInstanceOf(IpdPermissionException.class);

        verify(correctionLogMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("参数：listByEntity entityType/entityId 缺失抛 PARAM_INVALID")
    void listByEntity_paramValidation() {
        when(permission.requireAdmin()).thenReturn(new IpdActor(9001L, "admin", "SUPER_ADMIN", 0L));

        assertThatThrownBy(() -> service.listByEntity(null, 101L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        assertThatThrownBy(() -> service.listByEntity("ProjectScore", null))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        assertThatThrownBy(() -> service.listByEntity("", 101L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        verify(correctionLogMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }
}
