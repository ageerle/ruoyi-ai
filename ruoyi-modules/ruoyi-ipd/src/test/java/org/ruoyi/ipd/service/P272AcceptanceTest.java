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
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.HandoverRecord;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.support.NoopTransactionManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-7.2 批量移交与失败补偿验收（AC-HAND-04）。
 *
 * <p>正反例口径：
 * <ul>
 *   <li>批量移交 3 个项目给新 PM ⇒ 逐项目 COMPLETED，绑定转移、项目历史零改写（跟随）</li>
 *   <li>失败项目保持原归属：归属不符/校验拒绝仅该项目 REJECTED，不写库，不影响其余</li>
 *   <li>重试不重复成功项：已移交项目 SKIPPED_ALREADY_HANDED_OVER，不重写</li>
 *   <li>不得提前禁用：仍有待移交人员（余留活跃绑定）时绝不 DISABLED</li>
 *   <li>真全清（本批移交后活跃绑定计数=0）⇒ DISABLED + 企微解绑（AC-HAND-01d 联动）</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P272AcceptanceTest {

    @Mock
    private ProjectMemberMapper memberMapper;
    @Mock
    private PersonMapper personMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private HandoverMapper handoverMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;

    private ProjectMemberService projectMemberService;
    private HandoverService handoverService;

    private static final IpdActor GROUP_LEAD = new IpdActor(900L, "产品组长", "GROUP_LEADER", 7L);
    private static final long FROM_ID = 101L;
    private static final long TO_ID = 201L;
    /** P1 不与 FROM_ID=101 同值：重试幂等 stub 靠 projectId 值区分上轮/本轮。 */
    private static final long P1 = 111L;
    private static final long P2 = 102L;
    private static final long P3 = 103L;

    /** 已退出绑定数（memberMapper.update 调用次数）——驱动 disableIfAllCleared 的真全清判定。 */
    private final AtomicInteger exited = new AtomicInteger();

    @BeforeAll
    static void initMybatisMeta() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "P272-handover");
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
        TableInfoHelper.initTableInfo(assistant, HandoverRecord.class);
        TableInfoHelper.initTableInfo(assistant, Person.class);
    }

    @BeforeEach
    void setUp() {
        projectMemberService = new ProjectMemberService(memberMapper, personMapper, projectMapper,
            systemConfigService, auditLogService);
        handoverService = new HandoverService(memberMapper, personMapper, projectMapper, handoverMapper,
            auditLogService, projectMemberService, NoopTransactionManager.INSTANCE);
        lenient().when(projectMapper.selectById(anyLong())).thenReturn(projectWithGroup());
        lenient().when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(systemConfigService.getIntValue("allowance.projectCountThreshold", 3)).thenReturn(3);
        lenient().when(systemConfigService.getIntValue("allowance.L2", -1)).thenReturn(1500);
        lenient().when(personMapper.selectById(FROM_ID)).thenReturn(resignedRdPm());
        lenient().when(personMapper.selectById(TO_ID)).thenReturn(candidateRdPm());
        // exitForHandover：每项目 1 次 update，同时驱动全清计数
        lenient().doAnswer(inv -> exited.incrementAndGet()).when(memberMapper).update(any(), any());
        // disableIfAllCleared 全清禁用：person 侧条件 update（ACTIVE 守卫）放行 1 条
        lenient().when(personMapper.update(any(), any())).thenReturn(1);
        lenient().when(handoverMapper.selectCount(any())).thenReturn(0L);
        lenient().when(handoverMapper.insert(any(HandoverRecord.class))).thenReturn(1);
        lenient().when(handoverMapper.updateById(any(HandoverRecord.class))).thenReturn(1);
        lenient().when(memberMapper.insert(any(ProjectMember.class))).thenReturn(1);
        // selectOne：归属校验/在任反查——按 wrapper 中的 projectId 值分流
        lenient().when(memberMapper.selectOne(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            if (w == null) { return binding(FROM_ID, P1); }
            Long pid = wrapperProjectId(w);
            if (pid != null && pid == P2) { return binding(FROM_ID, P2); }
            if (pid != null && pid == P3) { return binding(FROM_ID, P3); }
            return binding(FROM_ID, P1);
        });
        // selectCount：paramNameValuePairs 填充时序在本环境不稳定（同一构造 map 时有时无），不再解析内容——
        // FOR UPDATE=接手人活跃计数恒 0；其余默认 1（重试幂等检查=活跃）。重复预检/重试的区分由各用例按
        // 确定性调用序列覆盖（每项目 handOverOne 重试检查 1 次，COMPLETED 项目 bindMember 预检 1 次）
        lenient().when(memberMapper.selectCount(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            if (w == null) { return 0L; }
            String seg = w.getSqlSegment();
            if (seg != null && seg.contains("FOR UPDATE")) { return 0L; }
            return 1L;
        });
        // selectList：FOR UPDATE=disableIfAllCleared 全清计数（exited 达到已移交总数才空）；其余=名下枚举
        lenient().when(memberMapper.selectList(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            if (w == null) { return List.of(); }
            String seg = w.getSqlSegment();
            if (seg != null && seg.contains("FOR UPDATE")) {
                return exited.get() >= 3 ? List.of() : List.of(binding(FROM_ID, P1));
            }
            return List.of(binding(FROM_ID, P1), binding(FROM_ID, P2), binding(FROM_ID, P3));
        });
    }

    // ---------- 造数 ----------

    private static Long P1L() { return P1; }
    private static Long P2L() { return P2; }
    private static Long P3L() { return P3; }

    /** 按调用序号从固定序列取 stub 返回值——业务调用序列确定（for 循环逐项目），避免解析 wrapper 内容。 */
    private static long seqValue(AtomicInteger cnt, long... values) {
        int i = cnt.getAndIncrement();
        return i < values.length ? values[i] : 0L;
    }

    /**
     * 从 LambdaQueryWrapper 的 ParamNameValuePair 中按字段名取值——AbstractWrapper.getParamNameValuePairs()
     * 实际返回 Map<String, Object>，值就是查询参数本身（Long 等）。不同 MP 版本对包装对象可能
     * 不一致（ParamNameValuePair vs 裸值），最稳是按字段名精确取。
     */
    private static Long wrapperProjectId(LambdaQueryWrapper<ProjectMember> w) {
        return wrapperLongValue(w, "project_id");
    }

    private static Long wrapperPersonId(LambdaQueryWrapper<ProjectMember> w) {
        return wrapperLongValue(w, "person_id");
    }

    private static Long wrapperLongValue(LambdaQueryWrapper<ProjectMember> w, String field) {
        var pairs = w.getParamNameValuePairs();
        for (var entry : pairs.entrySet()) {
            if (field.equals(entry.getKey())) {
                Object v = unwrapParamValue(entry.getValue());
                if (v == null) continue;
                if (v instanceof Long L) return L;
                if (v instanceof Number n) return n.longValue();
            }
        }
        return null;
    }

    /**
     * MP 3.5.x 偶有把值包成 ParamNameValuePair（无 getValue API，仅字段），用反射拿 property/value。
     * 兼容裸值与包装两种情形——只在 Mock 断言里用，业务代码无关。
     */
    private static Object unwrapParamValue(Object raw) {
        if (raw == null) return null;
        try {
            // 优先尝试 ParamNameValuePair.value 字段
            var f = raw.getClass().getDeclaredField("value");
            f.setAccessible(true);
            return f.get(raw);
        } catch (NoSuchFieldException e) {
            // 裸值
            return raw;
        } catch (Exception e) {
            return raw;
        }
    }

    private Person resignedRdPm() {
        Person p = new Person();
        p.setId(FROM_ID);
        p.setName("已离职研发PM");
        p.setPersonType("RD_PM");
        p.setLevel("L3");
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("RESIGNED");
        p.setWecomUserId("wecom-101");
        p.setDelFlag("0");
        return p;
    }

    private Person candidateRdPm() {
        Person p = new Person();
        p.setId(TO_ID);
        p.setName("接任研发PM");
        p.setPersonType("RD_PM");
        p.setLevel("L2");
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        p.setDelFlag("0");
        return p;
    }

    private ProjectMember binding(long personId, long projectId) {
        return ProjectMember.builder()
            .projectId(projectId).personId(personId).role("RD_PM")
            .memberType("PRIMARY").lockedLevel("L3").bonusEligible("1")
            .build();
    }

    private Project projectWithGroup() {
        Project p = new Project();
        p.setMainGroupId(7L);
        return p;
    }

    private List<HandoverService.BatchHandoverResult> batch(List<Long> projectIds) {
        return handoverService.batchHandover(FROM_ID, "RD_PM", TO_ID, "离职批量移交", null, projectIds, GROUP_LEAD);
    }

    // ---------- AC-HAND-04 主链 ----------

    @Test
    @DisplayName("AC-HAND-04：全量批量移交 3 个项目 ⇒ 逐项 COMPLETED，历史零改写，真全清才 DISABLED")
    void batchAllCompletedThenDisabled() {
        // selectCount 序列：每项目「重试检查=1、重复预检=0」交替 ×3
        AtomicInteger scCnt = new AtomicInteger();
        lenient().when(memberMapper.selectCount(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            if (w == null) { return 0L; }
            String seg = w.getSqlSegment();
            if (seg != null && seg.contains("FOR UPDATE")) { return 0L; }
            return seqValue(scCnt, 1L, 0L, 1L, 0L, 1L, 0L);
        });
        List<HandoverService.BatchHandoverResult> results = batch(null);

        assertThat(results).hasSize(3).allSatisfy(r -> {
            assertThat(r.status()).isEqualTo("COMPLETED");
            assertThat(r.reason()).isNull();
        });
        assertThat(results).extracting(HandoverService.BatchHandoverResult::projectId)
            .containsExactly(P1, P2, P3);
        // 每项目一张移交单 + 一次接手绑定
        verify(handoverMapper, times(3)).insert(any(HandoverRecord.class));
        verify(memberMapper, times(3)).insert(any(ProjectMember.class));
        // 项目本体/绑定历史零删除零改写（AC-HAND-04：审批、Gate、台账记录完整跟随）
        verify(projectMapper, never()).updateById(any(Project.class));
        verify(projectMapper, never()).deleteById(anyLong());
        // 审计 10 条：每项目（CREATE/BIND/ACCEPT）×3 + 全清 DISABLED ×1
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(10)).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .endsWith("HANDOVER_ACCEPT", "ACCOUNT_DISABLED_AFTER_HANDOVER");
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .filteredOn(a -> a.equals("ACCOUNT_DISABLED_AFTER_HANDOVER")).hasSize(1);
        // 禁用 update 是最后一条 person update：DISABLED + 企微解绑
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Person>> updCap =
            ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(personMapper, times(4)).update(any(), updCap.capture());
        var disable = updCap.getAllValues().get(3);
        assertThat(disable.getParamNameValuePairs().values()).contains("DISABLED");
    }

    @Test
    @DisplayName("失败项目保持原归属：第 2 个项目归属不符 ⇒ 仅它 REJECTED 不写库，其余照常完成")
    void batchPartialFailureKeepsOwnership() {
        // 归属校验调用序列（业务确定）：P1 归属、P1 内部反查、P2 归属、P3 归属、P3 内部反查——第 3 次=P2
        AtomicInteger soCnt = new AtomicInteger();
        lenient().when(memberMapper.selectOne(any())).thenAnswer(inv -> {
            if (inv.getArgument(0) == null) { return binding(FROM_ID, P1); }
            return soCnt.getAndIncrement() == 2 ? binding(999L, P2) : binding(FROM_ID, P1);
        });
        // selectCount 序列：重试P1=1、预检P1=0、重试P2=1（P2 归属被拒无预检）、重试P3=1、预检P3=0
        AtomicInteger scCnt = new AtomicInteger();
        lenient().when(memberMapper.selectCount(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            if (w == null) { return 0L; }
            String seg = w.getSqlSegment();
            if (seg != null && seg.contains("FOR UPDATE")) { return 0L; }
            return seqValue(scCnt, 1L, 0L, 1L, 1L, 0L);
        });

        List<HandoverService.BatchHandoverResult> results = batch(List.of(P1, P2, P3));

        assertThat(results).extracting(HandoverService.BatchHandoverResult::status)
            .containsExactly("COMPLETED", "REJECTED", "COMPLETED");
        assertThat(results.get(1).reason()).contains("与指定原负责人不符").contains("保持原归属");
        // 仅 2 个项目走完移交写路径；P2 无移交单、无绑定写入
        verify(handoverMapper, times(2)).insert(any(HandoverRecord.class));
        verify(memberMapper, times(2)).insert(any(ProjectMember.class));
        // P2 仍活跃（余留绑定）⇒ 无人被禁用：3 次 freeze 之外无 disable update
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(6)).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .doesNotContain("ACCOUNT_DISABLED_AFTER_HANDOVER");
    }

    @Test
    @DisplayName("重试不重复成功项：P1 上轮已移交 ⇒ SKIPPED 不重写；P2 正常完成")
    void batchRetrySkipsCompleted() {
        // selectCount 序列：重试P1=0（上轮已移交 SKIPPED）、重试P2=1、预检P2=0
        AtomicInteger scCnt = new AtomicInteger();
        lenient().when(memberMapper.selectCount(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            if (w == null) { return 0L; }
            String seg = w.getSqlSegment();
            if (seg != null && seg.contains("FOR UPDATE")) { return 0L; }
            return seqValue(scCnt, 0L, 1L, 0L);
        });
        // P2 移交后真全清（P1 上轮已清 + P2 本轮清）⇒ DISABLED
        lenient().when(memberMapper.selectList(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            if (w == null) { return List.of(); }
            String seg = w.getSqlSegment();
            if (seg != null && seg.contains("FOR UPDATE")) {
                return exited.get() >= 1 ? List.of() : List.of(binding(FROM_ID, P2));
            }
            return List.of(binding(FROM_ID, P1), binding(FROM_ID, P2));
        });

        List<HandoverService.BatchHandoverResult> results = batch(List.of(P1, P2));

        assertThat(results).extracting(HandoverService.BatchHandoverResult::status)
            .containsExactly("SKIPPED_ALREADY_HANDED_OVER", "COMPLETED");
        assertThat(results.get(0).reason()).contains("上轮移交已成功");
        // 只为 P2 写移交单；P1 零写入
        ArgumentCaptor<HandoverRecord> recCap = ArgumentCaptor.forClass(HandoverRecord.class);
        verify(handoverMapper, times(1)).insert(recCap.capture());
        assertThat(recCap.getValue().getProjectId()).isEqualTo(P2);
    }

    @Test
    @DisplayName("不得提前禁用：清单只含部分项目，名下仍有清单外活跃绑定 ⇒ 完成后也不 DISABLED")
    void batchNoPrematureDisable() {
        // selectCount 序列：重试P1=1、预检P1=0
        AtomicInteger scCnt = new AtomicInteger();
        lenient().when(memberMapper.selectCount(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            if (w == null) { return 0L; }
            String seg = w.getSqlSegment();
            if (seg != null && seg.contains("FOR UPDATE")) { return 0L; }
            return seqValue(scCnt, 1L, 0L);
        });
        lenient().when(memberMapper.selectList(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            if (w == null) { return List.of(); }
            String seg = w.getSqlSegment();
            if (seg != null && seg.contains("FOR UPDATE")) {
                return List.of(binding(FROM_ID, P2)); // P2 清单外，始终活跃
            }
            return List.of(binding(FROM_ID, P1));
        });

        List<HandoverService.BatchHandoverResult> results = batch(List.of(P1));

        assertThat(results).extracting(HandoverService.BatchHandoverResult::status)
            .containsExactly("COMPLETED");
        // 仅 freeze 一次 update，无 disable 分支
        verify(personMapper, times(1)).update(any(), any());
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(3)).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .doesNotContain("ACCOUNT_DISABLED_AFTER_HANDOVER");
    }

    @Test
    @DisplayName("名下该角色零活跃绑定且未指定清单 ⇒ 返回空清单，零写库")
    void batchEmptyScopeReturnsEmpty() {
        lenient().when(memberMapper.selectList(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            if (w == null) { return List.of(); }
            String seg = w.getSqlSegment();
            return seg != null && seg.contains("FOR UPDATE") ? List.of() : List.of();
        });

        List<HandoverService.BatchHandoverResult> results = batch(null);

        assertThat(results).isEmpty();
        verify(handoverMapper, never()).insert(any(HandoverRecord.class));
        verify(memberMapper, never()).update(any(), any());
    }

    // ---------- 入口校验 ----------

    @Test
    @DisplayName("角色越界（非 MARKET_PM/RD_PM）⇒ 整批拒绝，零写库")
    void batchIllegalRoleThrows() {
        assertThatThrownBy(() -> handoverService.batchHandover(
                FROM_ID, "DEV", TO_ID, "非法角色", null, List.of(P1), GROUP_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("角色非法");
        verify(handoverMapper, never()).insert(any(HandoverRecord.class));
    }

    @Test
    @DisplayName("接手人与原负责人相同 ⇒ 整批拒绝")
    void batchSelfHandoverThrows() {
        assertThatThrownBy(() -> handoverService.batchHandover(
                FROM_ID, "RD_PM", FROM_ID, "自移交", null, List.of(P1), GROUP_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("原负责人缺失或与接手人相同");
        verify(handoverMapper, never()).insert(any(HandoverRecord.class));
    }
}
