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
import org.ruoyi.ipd.domain.AuditLog;
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
 * P3-2.2 CorrectionLogService 单测（5 测覆盖 4 维度）。
 *
 * <p>R-P3-2.2-POSTREVIEW：新增 6 测覆盖 security review 修复：
 * <ol>
 *   <li>entityType 非枚举值 → PARAM_INVALID</li>
 *   <li>fieldName 超 128 → PARAM_INVALID</li>
 *   <li>newValue 超 4096 → PARAM_INVALID</li>
 *   <li>reason 含控制字符 → PARAM_INVALID</li>
 *   <li>全字段 trim（leading/trailing space）</li>
 *   <li>listByEntity admin 读触发 audit_log 写入（READ action / SUPER_ADMIN 角色）</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class CorrectionLogServiceTest {

    @Mock private CorrectionLogMapper correctionLogMapper;
    @Mock private IpdPermission permission;
    @Mock private AuditLogService auditLogService;

    private CorrectionLogService service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p322-correction-test"),
            CorrectionLog.class);
    }

    @BeforeEach
    void setUp() {
        service = new CorrectionLogService(correctionLogMapper, permission, auditLogService);
    }

    private IpdActor actor() {
        return new IpdActor(8001L, "operator-1", "GROUP_LEADER", 20L);
    }

    @Test
    @DisplayName("正常：record() 写入一行 CorrectionLog，字段全部透传")
    void record_writesRowWithFullFields() {
        service.record(actor(), "PROJECT_SCORE", 101L, "weightedScore",
            "85.50", "88.00", "终态更正");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<CorrectionLog> cap = ArgumentCaptor.forClass(CorrectionLog.class);
        verify(correctionLogMapper, times(1)).insert(cap.capture());
        CorrectionLog row = cap.getValue();
        assertThat(row.getEntityType()).isEqualTo("PROJECT_SCORE");
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
        service.record(actor(), "PROJECT_SCORE", 101L, "weightedScore",
            "85.50", "85.50", "no-op");

        verify(correctionLogMapper, never()).insert(any(CorrectionLog.class));
    }

    @Test
    @DisplayName("异常：必填字段缺失一律抛 PARAM_INVALID，不入库")
    void record_requiredFieldsValidated() {
        IpdActor a = actor();

        // actor null
        assertThatThrownBy(() -> service.record(null, "PROJECT_SCORE", 1L, "f", "o", "n", "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);

        // entityType blank
        assertThatThrownBy(() -> service.record(a, "  ", 1L, "f", "o", "n", "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // entityId null
        assertThatThrownBy(() -> service.record(a, "PROJECT_SCORE", null, "f", "o", "n", "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // fieldName blank
        assertThatThrownBy(() -> service.record(a, "PROJECT_SCORE", 1L, "", "o", "n", "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // newValue null
        assertThatThrownBy(() -> service.record(a, "PROJECT_SCORE", 1L, "f", "o", null, "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        // reason blank
        assertThatThrownBy(() -> service.record(a, "PROJECT_SCORE", 1L, "f", "o", "n", "  "))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        verify(correctionLogMapper, never()).insert(any(CorrectionLog.class));
    }

    @Test
    @DisplayName("读取：listByEntity 仅超管；透传 selectList（operatedAt DESC）")
    void listByEntity_adminOnlyAndPassesThrough() {
        IpdActor admin = new IpdActor(9001L, "admin", "SUPER_ADMIN", 0L);
        when(permission.requireAdmin()).thenReturn(admin);
        CorrectionLog r1 = CorrectionLog.builder().id(2L).entityType("PROJECT_SCORE").entityId(101L)
            .fieldName("weightedScore").oldValue("o").newValue("n").reason("r")
            .operatorId(8001L).operatorName("op").operatedAt(new java.util.Date()).build();
        CorrectionLog r2 = CorrectionLog.builder().id(1L).entityType("PROJECT_SCORE").entityId(101L)
            .fieldName("weightedScore").oldValue("o2").newValue("n2").reason("r2")
            .operatorId(8002L).operatorName("op2").operatedAt(new java.util.Date(0L)).build();
        when(correctionLogMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(r1, r2));

        List<CorrectionLog> result = service.listByEntity("PROJECT_SCORE", 101L, admin);

        assertThat(result).hasSize(2);
        verify(permission, times(1)).requireAdmin();
        verify(correctionLogMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        // meta-audit 必触发：READ 动作 + 传入 actor
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("权限：listByEntity 非超管抛 403；不触达 mapper")
    void listByEntity_adminOnly() {
        when(permission.requireAdmin())
            .thenThrow(new IpdPermissionException(403, ApiV1ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> service.listByEntity("PROJECT_SCORE", 101L, actor()))
            .isInstanceOf(IpdPermissionException.class);

        verify(correctionLogMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("参数：listByEntity entityType/entityId 缺失抛 PARAM_INVALID")
    void listByEntity_paramValidation() {
        IpdActor admin = new IpdActor(9001L, "admin", "SUPER_ADMIN", 0L);
        when(permission.requireAdmin()).thenReturn(admin);

        assertThatThrownBy(() -> service.listByEntity(null, 101L, admin))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        assertThatThrownBy(() -> service.listByEntity("PROJECT_SCORE", null, admin))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        assertThatThrownBy(() -> service.listByEntity("", 101L, admin))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);

        verify(correctionLogMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    // ============== R-P3-2.2-POSTREVIEW 新增测 ==============

    @Test
    @DisplayName("POSTREVIEW-1：entityType 非枚举值（free string）抛 PARAM_INVALID")
    void record_entityTypeNotInEnum_rejected() {
        IpdActor a = actor();
        assertThatThrownBy(() -> service.record(a, "FreeStringNotInEnum", 1L, "f", "o", "n", "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verify(correctionLogMapper, never()).insert(any(CorrectionLog.class));
    }

    @Test
    @DisplayName("POSTREVIEW-2：fieldName 超过 128 字符上限抛 PARAM_INVALID")
    void record_fieldNameOver128_rejected() {
        IpdActor a = actor();
        String tooLong = "x".repeat(129);
        assertThatThrownBy(() -> service.record(a, "PROJECT_SCORE", 1L, tooLong, "o", "n", "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verify(correctionLogMapper, never()).insert(any(CorrectionLog.class));
    }

    @Test
    @DisplayName("POSTREVIEW-3：newValue 超过 4096 字符上限抛 PARAM_INVALID")
    void record_newValueOver4096_rejected() {
        IpdActor a = actor();
        String tooLong = "x".repeat(4097);
        assertThatThrownBy(() -> service.record(a, "PROJECT_SCORE", 1L, "f", "o", tooLong, "r"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verify(correctionLogMapper, never()).insert(any(CorrectionLog.class));
    }

    @Test
    @DisplayName("POSTREVIEW-4：reason 含控制字符（非 \\t\\n\\r）抛 PARAM_INVALID")
    void record_reasonHasControlChar_rejected() {
        IpdActor a = actor();
        String withCtrl = "ok\u0007"; // BEL 字符
        assertThatThrownBy(() -> service.record(a, "PROJECT_SCORE", 1L, "f", "o", "n", withCtrl))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verify(correctionLogMapper, never()).insert(any(CorrectionLog.class));
    }

    @Test
    @DisplayName("POSTREVIEW-5：全字段 trim（leading/trailing space 不污染写入）")
    void record_allFieldsTrimmed() {
        service.record(actor(),
            "  PROJECT_SCORE  ", 101L, "  weightedScore  ",
            "  85.50  ", "  88.00  ", "  终态更正  ");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<CorrectionLog> cap = ArgumentCaptor.forClass(CorrectionLog.class);
        verify(correctionLogMapper, times(1)).insert(cap.capture());
        CorrectionLog row = cap.getValue();
        assertThat(row.getEntityType()).isEqualTo("PROJECT_SCORE");
        assertThat(row.getFieldName()).isEqualTo("weightedScore");
        assertThat(row.getOldValue()).isEqualTo("85.50");
        assertThat(row.getNewValue()).isEqualTo("88.00");
        assertThat(row.getReason()).isEqualTo("终态更正");
    }

    @Test
    @DisplayName("POSTREVIEW-6：listByEntity admin 读触发 audit_log（action=READ, role=SUPER_ADMIN）")
    void listByEntity_appendsReadAudit() {
        IpdActor admin = new IpdActor(9001L, "admin", "SUPER_ADMIN", 0L);
        when(permission.requireAdmin()).thenReturn(admin);
        when(correctionLogMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of());

        service.listByEntity("KPI_RECORD", 555L, admin);

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        AuditLog audit = cap.getValue();
        assertThat(audit.getAction()).isEqualTo("READ");
        assertThat(audit.getOperatorRole()).isEqualTo("SUPER_ADMIN");
        assertThat(audit.getEntityType()).isEqualTo("KPI_RECORD");
        assertThat(audit.getEntityId()).isEqualTo(555L);
        assertThat(audit.getOperatorName()).isEqualTo("admin");
    }
}
