package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.ruoyi.ipd.domain.KpiRuleSnapshot;
import org.ruoyi.ipd.mapper.KpiRuleSnapshotMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionException;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P3-2.2 KpiRecordRuleVersionService 单测（7 测覆盖 6 维度）：
 * <ol>
 *   <li>正常路径：snapshotRuleVersion 入库新版本并返回递增版本号；首次 = 1</li>
 *   <li>边界：effectiveFrom 为空时自动赋当前时间</li>
 *   <li>异常：ruleJson 为空 / 重复内容抛业务异常</li>
 *   <li>权限：snapshotRuleVersion/getRuleVersion/listVersions 均经 requireAdmin</li>
 *   <li>幂等：getRuleVersion 不存在抛 NOT_FOUND</li>
 *   <li>前置闭合：snapshot 时将上一版本 effectiveTo 设为新版本 effectiveFrom</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class KpiRecordRuleVersionServiceTest {

    @Mock private KpiRuleSnapshotMapper snapshotMapper;
    @Mock private IpdPermission permission;

    private KpiRecordRuleVersionService service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p322-test"),
            KpiRuleSnapshot.class);
    }

    @BeforeEach
    void setUp() {
        service = new KpiRecordRuleVersionService(snapshotMapper, permission);
    }

    private IpdActor admin() {
        return new IpdActor(9001L, "admin", "SUPER_ADMIN", 0L);
    }

    private KpiRuleSnapshot input(String ruleJson) {
        return KpiRuleSnapshot.builder()
            .ruleJson(ruleJson)
            .effectiveFrom(new Date(1_700_000_000_000L))
            .createdBy("by-test")
            .build();
    }

    private KpiRuleSnapshot stored(long id, long version, String json, Date effectiveFrom) {
        KpiRuleSnapshot s = KpiRuleSnapshot.builder()
            .id(id).version(version).ruleJson(json)
            .effectiveFrom(effectiveFrom).effectiveTo(null)
            .createdBy("prev").createdAt(new Date())
            .build();
        return s;
    }

    @Test
    @DisplayName("正常：首条快照返回 version=1；调用一次 insert；闭合路径无 UPDATE（无前驱）")
    void snapshot_firstVersion_returnsOne() {
        when(permission.requireAdmin()).thenReturn(admin());
        when(snapshotMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Long version = service.snapshotRuleVersion(input("{\"self\":0.6,\"market\":0.4}"));

        assertThat(version).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<KpiRuleSnapshot> cap = ArgumentCaptor.forClass(KpiRuleSnapshot.class);
        verify(snapshotMapper, times(1)).insert(cap.capture());
        assertThat(cap.getValue().getVersion()).isEqualTo(1L);
        assertThat(cap.getValue().getRuleJson()).isEqualTo("{\"self\":0.6,\"market\":0.4}");
        // 首条无前驱 → 不发 UPDATE
        verify(snapshotMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("正常：第二条快照返回 prev.version+1；闭合前驱 effectiveTo = 新 effectiveFrom")
    void snapshot_secondVersion_closesPrevEffectiveTo() {
        Date prevFrom = new Date(1_700_000_000_000L);
        KpiRuleSnapshot prev = stored(11L, 1L, "{\"self\":0.5,\"market\":0.5}", prevFrom);
        when(permission.requireAdmin()).thenReturn(admin());
        when(snapshotMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(prev);
        when(snapshotMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Long version = service.snapshotRuleVersion(input("{\"self\":0.6,\"market\":0.4}"));

        assertThat(version).isEqualTo(2L);
        // 前驱闭合：UPDATE 一次
        verify(snapshotMapper, times(1)).update(eq(null), any(LambdaUpdateWrapper.class));
        // 新版本入库：INSERT 一次
        verify(snapshotMapper, times(1)).insert(any(KpiRuleSnapshot.class));
    }

    @Test
    @DisplayName("异常：与当前生效规则 JSON 一致时拒绝创建新版本（NO-OP）")
    void snapshot_duplicateRule_rejected() {
        Date prevFrom = new Date(1_700_000_000_000L);
        KpiRuleSnapshot prev = stored(11L, 1L, "{\"self\":0.5}", prevFrom);
        when(permission.requireAdmin()).thenReturn(admin());
        when(snapshotMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(prev);

        KpiRuleSnapshot dup = input("{\"self\":0.5}");
        assertThatThrownBy(() -> service.snapshotRuleVersion(dup))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        verify(snapshotMapper, never()).insert(any(KpiRuleSnapshot.class));
    }

    @Test
    @DisplayName("异常：ruleJson 为空抛 PARAM_INVALID")
    void snapshot_blankRuleJson_rejected() {
        when(permission.requireAdmin()).thenReturn(admin());
        KpiRuleSnapshot blank = input("  ");
        assertThatThrownBy(() -> service.snapshotRuleVersion(blank))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verify(snapshotMapper, never()).insert(any(KpiRuleSnapshot.class));
    }

    @Test
    @DisplayName("权限：snapshot/getRuleVersion/listVersions 均先经 requireAdmin 守卫")
    void allEntryPoints_requireAdminFirst() {
        // snapshot 抛 403
        when(permission.requireAdmin())
            .thenThrow(new IpdPermissionException(403, ApiV1ErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> service.snapshotRuleVersion(input("{}")))
            .isInstanceOf(IpdPermissionException.class);
        // getRuleVersion 抛 403
        assertThatThrownBy(() -> service.getRuleVersion(1L))
            .isInstanceOf(IpdPermissionException.class);
        // listVersions 抛 403
        assertThatThrownBy(() -> service.listVersions())
            .isInstanceOf(IpdPermissionException.class);
        verify(snapshotMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(snapshotMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(snapshotMapper, never()).insert(any(KpiRuleSnapshot.class));
    }

    @Test
    @DisplayName("读取：getRuleVersion 不存在抛 NOT_FOUND；存在则透传")
    void getRuleVersion_notFound() {
        when(permission.requireAdmin()).thenReturn(admin());
        when(snapshotMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThatThrownBy(() -> service.getRuleVersion(99L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("读取：listVersions 透传 selectList（version DESC）")
    void listVersions_passesThrough() {
        when(permission.requireAdmin()).thenReturn(admin());
        KpiRuleSnapshot v2 = stored(22L, 2L, "{}", new Date());
        KpiRuleSnapshot v1 = stored(11L, 1L, "{}", new Date());
        when(snapshotMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(v2, v1));

        List<KpiRuleSnapshot> result = service.listVersions();

        assertThat(result).hasSize(2).first().extracting(KpiRuleSnapshot::getVersion).isEqualTo(2L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<KpiRuleSnapshot>> cap =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(snapshotMapper).selectList(cap.capture());
    }

    // ============== R-P3-2.2-POSTREVIEW 新增测 ==============

    @Test
    @DisplayName("POSTREVIEW-1：snapshot insert 显式写 tenantId='000000'（治理元数据表）")
    void snapshot_insertWritesExplicitTenantId() {
        when(permission.requireAdmin()).thenReturn(admin());
        when(snapshotMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.snapshotRuleVersion(input("{\"self\":0.6,\"market\":0.4}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<KpiRuleSnapshot> cap = ArgumentCaptor.forClass(KpiRuleSnapshot.class);
        verify(snapshotMapper, times(1)).insert(cap.capture());
        // 治理元数据表（与 system_config_versions 同语义）：tenantId 必须显式为 000000
        assertThat(cap.getValue().getTenantId()).isEqualTo("000000");
    }

    @Test
    @DisplayName("POSTREVIEW-2：ruleJson 超过 64KiB 上限抛 PARAM_INVALID")
    void snapshot_ruleJsonOver64K_rejected() {
        when(permission.requireAdmin()).thenReturn(admin());
        String tooLong = "x".repeat(65537);
        KpiRuleSnapshot big = input(tooLong);
        assertThatThrownBy(() -> service.snapshotRuleVersion(big))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verify(snapshotMapper, never()).insert(any(KpiRuleSnapshot.class));
    }
}
