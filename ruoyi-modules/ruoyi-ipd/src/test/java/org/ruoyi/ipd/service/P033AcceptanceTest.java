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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.domain.SystemConfigVersion;
import org.ruoyi.ipd.mapper.SystemConfigMapper;
import org.ruoyi.ipd.mapper.SystemConfigVersionMapper;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-3.3 参数版本持久化、不可变快照与时点解析验收（AC-GLB-09/10；G-05/G-08）。
 *
 * <p>形态为 Mockito 单元验收（对齐 P032/P231 惯例）；真库表结构探针见
 * docs/ipd-系统说明/验收/P0-3.3-参数版本链-验收证据-20260905.md。
 * <p>不得据此单测绿标 done（BR-真库：HTTP/真库写入验证未过只可 inreview）。
 */
@Tag("dev")
@DisplayName("P033 参数版本链：持久化/不可变快照/时点解析")
@ExtendWith(MockitoExtension.class)
class P033AcceptanceTest {

    @Mock
    private SystemConfigMapper systemConfigMapper;

    @Mock
    private SystemConfigVersionMapper systemConfigVersionMapper;

    @InjectMocks
    private SystemConfigService service;

    private SystemConfig configRow;

    @BeforeAll
    static void initTableInfo() {
        // 生产环境由 MyBatis mapper 注册时初始化 TableInfo；纯 Mockito 单测 JVM 无此环节，
        // 而 Wrappers.lambdaUpdate().set(...) 的列解析是即时路径 → 需显式初始化 lambda 缓存。
        // 与其改用字符串列绕过，不如初始化后走生产同构的 lambda 闭包路径（对齐 DefectB「真实链」哲学）。
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), ""), SystemConfigVersion.class);
    }

    @BeforeEach
    void setUp() {
        configRow = SystemConfig.builder()
            .id(1L).configKey("bonus.salesSource").configValue("BOOKING")
            .valueType("STRING").defaultValue("BOOKING")
            .build();
        configRow.setCreateTime(new Date(System.currentTimeMillis() - 90L * 24 * 3600 * 1000));
    }

    private SystemConfigVersion versionRow(int version, String value, Date from, Date to) {
        return SystemConfigVersion.builder()
            .id((long) version).configKey("bonus.salesSource").configValue(value)
            .version(version).effectiveFrom(from).effectiveTo(to)
            .isImmutable(Boolean.TRUE).changedBy(9L)
            .build();
    }

    @Test
    @DisplayName("首次更新落基线行 v1(旧值,回溯锚)+新值行 v2；changed_by 绑会话；effective_to=NULL 开区间")
    void firstUpdate_writesBaselineAndNewVersion() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(configRow);
        when(systemConfigVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.update("bonus.salesSource", "SHIPMENT", 9L);

        ArgumentCaptor<SystemConfigVersion> captor = ArgumentCaptor.forClass(SystemConfigVersion.class);
        verify(systemConfigVersionMapper, times(2)).insert(captor.capture());
        List<SystemConfigVersion> inserted = captor.getAllValues();

        SystemConfigVersion baseline = inserted.get(0);
        assertThat(baseline.getVersion()).isEqualTo(1);
        assertThat(baseline.getConfigValue()).isEqualTo("BOOKING");
        assertThat(baseline.getEffectiveFrom()).isEqualTo(configRow.getCreateTime());
        assertThat(baseline.getEffectiveTo()).isNotNull();
        assertThat(baseline.getChangedBy()).isEqualTo(9L);
        assertThat(baseline.getIsImmutable()).isTrue();

        SystemConfigVersion latest = inserted.get(1);
        assertThat(latest.getVersion()).isEqualTo(2);
        assertThat(latest.getConfigValue()).isEqualTo("SHIPMENT");
        assertThat(latest.getEffectiveTo()).isNull();
        assertThat(latest.getChangedBy()).isEqualTo(9L);
    }

    @Test
    @DisplayName("后续更新：闭合当前开区间行(仅 effective_to) + 追加 version=last+1")
    void subsequentUpdate_closesOpenRowAndAppendsNext() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(configRow);
        SystemConfigVersion v2 = versionRow(2, "SHIPMENT",
            new Date(System.currentTimeMillis() - 3600_000L), null);
        when(systemConfigVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(v2);

        service.update("bonus.salesSource", "DELIVERED", 7L);

        // 闭合：仅一次受控 lambdaUpdate（null entity + wrapper），无 updateById(entity)
        verify(systemConfigVersionMapper).update(isNull(), any());
        verify(systemConfigVersionMapper, never()).updateById(any(SystemConfigVersion.class));

        ArgumentCaptor<SystemConfigVersion> captor = ArgumentCaptor.forClass(SystemConfigVersion.class);
        verify(systemConfigVersionMapper, times(1)).insert(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(3);
        assertThat(captor.getValue().getConfigValue()).isEqualTo("DELIVERED");
        assertThat(captor.getValue().getChangedBy()).isEqualTo(7L);
    }

    @Test
    @DisplayName("update 不存在的 key：静默忽略且不写任何版本行")
    void updateUnknownKey_noVersionRows() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.update("never.existed", "x", 9L);

        verify(systemConfigVersionMapper, never()).insert(any(SystemConfigVersion.class));
        verify(systemConfigVersionMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("遗留 2 参通道：changed_by 落 0=系统通道（真库 NOT NULL 兼容）")
    void legacyTwoArgUpdate_changedByZero() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(configRow);
        when(systemConfigVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.update("bonus.salesSource", "SHIPMENT");

        ArgumentCaptor<SystemConfigVersion> captor = ArgumentCaptor.forClass(SystemConfigVersion.class);
        verify(systemConfigVersionMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(v -> assertThat(v.getChangedBy()).isEqualTo(0L));
    }

    @Test
    @DisplayName("A 级必做集 key：归一化后值进入版本行（P1-5.2 契约不回退）")
    void aLevelKey_normalizedValueVersioned() throws Exception {
        SystemConfig aLevel = SystemConfig.builder()
            .id(2L).configKey(GateEngine.A_LEVEL_CONFIG_KEY).configValue("C11")
            .valueType("JSON").defaultValue("[]")
            .build();
        aLevel.setCreateTime(new Date());
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(aLevel);
        when(systemConfigVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // 走真实归一化（trim/去重/别名归一），非 mock；合法码锚点对齐 P152AcceptanceTest（Z03→C12）
        service.update(GateEngine.A_LEVEL_CONFIG_KEY, " Z03 , Z03 , C11 ", 9L);

        ArgumentCaptor<SystemConfigVersion> captor = ArgumentCaptor.forClass(SystemConfigVersion.class);
        verify(systemConfigVersionMapper, times(2)).insert(captor.capture());
        SystemConfigVersion latest = captor.getAllValues().get(1);
        assertThat(latest.getConfigValue()).isEqualTo("C12,C11");
    }

    @Test
    @DisplayName("时点解析谓词锁定：from<=T AND (to IS NULL OR to>T) ORDER BY version DESC（CodeReview M-2，Qa04 手法）")
    void asOf_queryPredicateLocked() {
        Date asOf = new Date(1_750_000_000_000L);
        when(systemConfigVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(configRow);

        service.getValueAsOf("bonus.salesSource", asOf);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<SystemConfigVersion>> captor =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(systemConfigVersionMapper).selectOne(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        // 半开区间 [from, to)：le 而非 lt，gt 而非 ge；边界 T==from 命中、T==to 被排除
        assertThat(sql)
            .contains("config_key")
            .contains("effective_from")
            .contains("<=")
            .contains("effective_to")
            .contains("IS NULL")
            .contains("ORDER BY version DESC");
        assertThat(sql).doesNotContain(">=");
        assertThat(sql).doesNotContain("<>");
        assertThat(captor.getValue().getParamNameValuePairs().values()).contains(asOf);
    }

    @Test
    @DisplayName("时点解析（行为样例）：T 落在闭区间行 → 返回该版本值")
    void asOf_withinClosedInterval_returnsThatVersion() {
        Date t0 = new Date(1_000_000_000L);
        Date t1 = new Date(2_000_000_000L);
        // v1: [t0, t1) BOOKING；T 落在区间内
        SystemConfigVersion v1 = versionRow(1, "BOOKING", t0, t1);
        when(systemConfigVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(v1);

        assertThat(service.getValueAsOf("bonus.salesSource", new Date(1_500_000_000L))).isEqualTo("BOOKING");
    }

    @Test
    @DisplayName("版本历史谓词：listVersions 按 version 降序（最新在前）")
    void listVersions_orderPredicateLocked() {
        when(systemConfigVersionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of(
            versionRow(3, "DELIVERED", new Date(), null),
            versionRow(2, "SHIPMENT", new Date(), new Date())));

        java.util.List<SystemConfigVersion> history = service.listVersions("bonus.salesSource", 20);

        assertThat(history).extracting(SystemConfigVersion::getVersion).containsExactly(3, 2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<SystemConfigVersion>> captor =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(systemConfigVersionMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("ORDER BY version DESC");
    }

    @Test
    @DisplayName("时点解析：T 早于全部版本 → 回退出厂默认 defaultValue（FACTORY_DEFAULT）")
    void asOf_beforeAllVersions_fallsBackToFactoryDefault() {
        when(systemConfigVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(configRow);

        assertThat(service.getValueAsOf("bonus.salesSource", new Date(0L))).isEqualTo("BOOKING");

        var view = service.resolveAsOf("bonus.salesSource", new Date(0L));
        assertThat(view.get("resolvedFrom")).isEqualTo("FACTORY_DEFAULT");
        assertThat(view.get("value")).isEqualTo("BOOKING");
    }

    @Test
    @DisplayName("时点解析：无版本且本体行缺 defaultValue → NONE（value=null）")
    void asOf_noDataAtAll_returnsNull() {
        when(systemConfigVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        SystemConfig bare = SystemConfig.builder()
            .id(3L).configKey("ghost.key").configValue("x").defaultValue(null).build();
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(bare);

        assertThat(service.getValueAsOf("ghost.key", new Date(0L))).isNull();
        assertThat(service.resolveAsOf("ghost.key", new Date(0L)).get("resolvedFrom")).isEqualTo("NONE");
    }

    @Test
    @DisplayName("版本历史（行为样例）：降序透传（原 listVersions_descending 契约保留）")
    void listVersions_descending() {
        when(systemConfigVersionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            versionRow(3, "DELIVERED", new Date(), null),
            versionRow(2, "SHIPMENT", new Date(), new Date())));

        List<SystemConfigVersion> history = service.listVersions("bonus.salesSource", 20);

        assertThat(history).extracting(SystemConfigVersion::getVersion).containsExactly(3, 2);
    }

    @Test
    @DisplayName("同值短路（CodeReview L-1）：update 传入旧值 → 不写本体、不写版本链")
    void sameValueUpdate_shortCircuits() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(configRow);

        service.update("bonus.salesSource", "BOOKING", 9L); // configRow 当前值即 BOOKING

        verify(systemConfigMapper, never()).updateById(any(SystemConfig.class));
        verify(systemConfigVersionMapper, never()).insert(any(SystemConfigVersion.class));
    }

    @Test
    @DisplayName("不可变快照行为守护：全流程零 deleteById / 零 updateById(版本行)")
    void appendOnly_noDeleteNoEntityUpdate() {
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(configRow);
        SystemConfigVersion v2 = versionRow(2, "SHIPMENT", new Date(), null);
        when(systemConfigVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(v2);

        service.update("bonus.salesSource", "DELIVERED", 9L);
        service.listVersions("bonus.salesSource", 5);
        service.getValueAsOf("bonus.salesSource", new Date());

        verify(systemConfigVersionMapper, never()).deleteById(any(java.io.Serializable.class));
        verify(systemConfigVersionMapper, never()).delete(any());
        verify(systemConfigVersionMapper, never()).updateById(any(SystemConfigVersion.class));
    }

    @Test
    @DisplayName("版本写入顺序：本体 updateById 先于版本 insert（CodeReview L-2 改名澄清）")
    void versionWriteFollowsBodyUpdateInSameCall() {
        // 单测只能锁同方法内的调用顺序；@Transactional 原子性（版本行与本体同归 rollback）
        // 属真库验收边界（BR-真库），见验收证据文档。
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(configRow);
        when(systemConfigVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.update("bonus.salesSource", "SHIPMENT", 9L);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(systemConfigMapper, systemConfigVersionMapper);
        order.verify(systemConfigMapper).updateById(any(SystemConfig.class));
        order.verify(systemConfigVersionMapper, times(2)).insert(any(SystemConfigVersion.class));
    }
}
