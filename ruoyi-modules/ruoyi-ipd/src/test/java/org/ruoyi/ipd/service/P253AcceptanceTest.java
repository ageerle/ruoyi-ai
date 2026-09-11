package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.domain.GateElementResult;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.ruoyi.ipd.mapper.GateElementResultMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.OssFileMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-5.3 Gate 条件遗留项、关闭凭证与阻断（单测层；真库 HTTP 验收另见验收文档）。
 *
 * <ul>
 *   <li>AC-GATE-16：CONDITIONAL 必填责任人 + 关闭期限（缺一拒绝提交判定）</li>
 *   <li>AC-GATE-17：逾期未关 → 通知责任人提醒 + 阻断下一 Gate 提交；要素停用/删除不消除遗留</li>
 *   <li>关闭需凭证，仅责任人/超管可关；CONDITIONAL→PASS 改判清空遗留（显式 set）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("dev")
@DisplayName("P2-5.3 条件遗留项：责任人与期限必填、关闭凭证、逾期提醒与阻断")
class P253AcceptanceTest {

    @Mock
    private GateMapper gateMapper;
    @Mock
    private GateElementMapper elementMapper;
    @Mock
    private GateElementResultMapper resultMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private OssFileMapper ossFileMapper;

    private GateElementResultService service;

    private static final IpdActor PM = new IpdActor(301L, "评审PM", "MARKET_PM", 7L);
    private static final IpdActor RESPONSIBLE = new IpdActor(900103L, "责任人PM", "MARKET_PM", 7L);
    private static final IpdActor ADMIN = new IpdActor(900101L, "超管", "SUPER_ADMIN", null);

    private Gate g1;
    private Gate g2;
    private GateElement g1Normal;   // G1-3 普通要素（条件判定载体）
    private GateElement g2e1;       // G2-1
    private GateElement g2e2;       // G2-2

    private Date yesterday;
    private Date tomorrow;

    @BeforeAll
    static void initMybatisMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P253-gate"), Gate.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P253-ger"), GateElementResult.class);
    }

    @BeforeEach
    void setUp() {
        service = new GateElementResultService(gateMapper, elementMapper, resultMapper,
            systemConfigService, auditLogService, notificationService, ossFileMapper);
        yesterday = new Date(System.currentTimeMillis() - 24L * 3600 * 1000);
        tomorrow = new Date(System.currentTimeMillis() + 24L * 3600 * 1000);
        g1 = gate(501L, "G1");
        g2 = gate(502L, "G2");
        g1Normal = element(603L, "G1", "G1-3", "0");
        g2e1 = element(701L, "G2", "G2-1", "0");
        g2e2 = element(702L, "G2", "G2-2", "0");
        lenient().when(gateMapper.selectById(501L)).thenReturn(g1);
        lenient().when(gateMapper.selectById(502L)).thenReturn(g2);
        lenient().when(elementMapper.selectById(603L)).thenReturn(g1Normal);
        lenient().when(elementMapper.selectList(any())).thenReturn(List.of(g2e1, g2e2));
        lenient().when(gateMapper.selectList(any())).thenReturn(List.of(g1));
        lenient().when(resultMapper.selectList(any())).thenReturn(List.of());
        // [SEC-FIX-HIGH-1.1-FOLLOWUP] oss mocks
        org.ruoyi.ipd.domain.OssFileEntity matOss = new org.ruoyi.ipd.domain.OssFileEntity();
        matOss.setOssId(9001L); matOss.setUrl("https://oss.local/materials/g2.pdf");
        org.ruoyi.ipd.domain.OssFileEntity minOss = new org.ruoyi.ipd.domain.OssFileEntity();
        minOss.setOssId(9002L); minOss.setUrl("https://oss.local/minutes/g2.pdf");
        lenient().when(ossFileMapper.selectById(9001L)).thenReturn(matOss);
        lenient().when(ossFileMapper.selectById(9002L)).thenReturn(minOss);
    }

    private Gate gate(Long id, String code) {
        Gate g = new Gate();
        g.setId(id);
        g.setProjectId(11L);
        g.setGateCode(code);
        g.setStatus("PENDING");
        g.setCurrentRound(1);
        return g;
    }

    private GateElement element(Long id, String gateCode, String code, String isVeto) {
        GateElement e = new GateElement();
        e.setId(id);
        e.setGateCode(gateCode);
        e.setElementCode(code);
        e.setElementName("要素" + code);
        e.setIsVeto(isVeto);
        e.setSortOrder(1);
        return e;
    }

    private GateElementResult conditionalRow(Long gateId, Long elementId, Date due) {
        return GateElementResult.builder()
            .id(9001L).gateId(gateId).elementId(elementId).result("CONDITIONAL")
            .conditionNote("补齐竞品对比").evidenceRef(null)
            .leftoverItem("补齐竞品对比").responsiblePersonId(900103L)
            .leftoverDueAt(due).leftoverStatus("OPEN")
            .build();
    }

    private GateElementResult judgedRow(Long gateId, Long elementId, String result) {
        return GateElementResult.builder()
            .id(9002L).gateId(gateId).elementId(elementId).result(result).build();
    }

    // ---------- AC-GATE-16：CONDITIONAL 必填责任人与关闭期限 ----------

    @Test
    @DisplayName("AC-GATE-16：CONDITIONAL 缺责任人 ⇒ 拒绝")
    void judge_conditional_responsibleMissing_rejected() {
        assertThatThrownBy(() -> service.judge(501L, 603L, "CONDITIONAL", "整改中", null, null, null,
            null, tomorrow, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("必须指定责任人");
        verify(resultMapper, never()).insert(any(GateElementResult.class));
    }

    @Test
    @DisplayName("AC-GATE-16：CONDITIONAL 缺关闭期限 ⇒ 拒绝")
    void judge_conditional_deadlineMissing_rejected() {
        assertThatThrownBy(() -> service.judge(501L, 603L, "CONDITIONAL", "整改中", null, null, null,
            900103L, null, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("必须填写关闭期限");
        verify(resultMapper, never()).insert(any(GateElementResult.class));
    }

    @Test
    @DisplayName("AC-GATE-16 正向：三件套齐全 ⇒ 遗留 OPEN 落库（责任人/期限/描述）")
    void judge_conditional_full_leftoverOpen() {
        when(resultMapper.selectOne(any())).thenReturn(null);

        GateElementResult row = service.judge(501L, 603L, "CONDITIONAL", "补齐竞品对比", null, null, null,
            900103L, tomorrow, PM);

        assertThat(row.getLeftoverStatus()).isEqualTo("OPEN");
        assertThat(row.getLeftoverItem()).isEqualTo("补齐竞品对比");
        assertThat(row.getResponsiblePersonId()).isEqualTo(900103L);
        assertThat(row.getLeftoverDueAt()).isEqualTo(tomorrow);
        verify(resultMapper).insert(any(GateElementResult.class));
    }

    @Test
    @DisplayName("CONDITIONAL→PASS 改判 ⇒ 遗留四件套显式清空（不走 updateById 忽略 null）")
    void judge_conditionalToPass_clearsLeftover() {
        GateElementResult existing = conditionalRow(501L, 603L, tomorrow);
        existing.setId(9001L);
        when(resultMapper.selectOne(any())).thenReturn(existing);

        GateElementResult row = service.judge(501L, 603L, "PASS", null, null, null, null, null, null, PM);

        assertThat(row.getResult()).isEqualTo("PASS");
        assertThat(row.getLeftoverStatus()).isNull();
        assertThat(row.getResponsiblePersonId()).isNull();
        assertThat(row.getLeftoverDueAt()).isNull();
        verify(resultMapper).update(any(), any());
        verify(resultMapper, never()).updateById(any(GateElementResult.class));
    }

    // ---------- 关闭遗留：凭证 + 权限 ----------

    @Test
    @DisplayName("关闭遗留缺凭证 ⇒ 拒绝")
    void close_evidenceRequired() {
        GateElementResult row = conditionalRow(501L, 603L, yesterday);
        when(resultMapper.selectById(9001L)).thenReturn(row);

        assertThatThrownBy(() -> service.close(501L, 9001L, " ", RESPONSIBLE))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("必须附凭证");
    }

    @Test
    @DisplayName("仅责任人/超管可关：无关 PM 拒，责任人过并留 LEGACY_CLOSE 审计")
    void close_permission_responsibleOrAdminOnly() {
        GateElementResult row = conditionalRow(501L, 603L, yesterday);
        when(resultMapper.selectById(9001L)).thenReturn(row);

        assertThatThrownBy(() -> service.close(501L, 9001L, "对比报告.pdf", PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("仅遗留责任人或超管");

        GateElementResult closed = service.close(501L, 9001L, "对比报告.pdf", RESPONSIBLE);
        assertThat(closed.getLeftoverStatus()).isEqualTo("CLOSED");
        assertThat(closed.getClosedEvidence()).isEqualTo("对比报告.pdf");
        verify(auditLogService).append(org.mockito.ArgumentMatchers.argThat(a ->
            a instanceof AuditLog && "LEGACY_CLOSE".equals(((AuditLog) a).getAction())));
    }

    @Test
    @DisplayName("已关闭的遗留不可重复关闭")
    void close_alreadyClosed_rejected() {
        GateElementResult row = conditionalRow(501L, 603L, yesterday);
        row.setLeftoverStatus("CLOSED");
        when(resultMapper.selectById(9001L)).thenReturn(row);

        assertThatThrownBy(() -> service.close(501L, 9001L, "再补一份.pdf", ADMIN))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不在开放状态");
    }

    // ---------- AC-GATE-17：逾期提醒 ----------

    @Test
    @DisplayName("AC-GATE-17 前半：逾期扫描 ⇒ 通知责任人 GATE_CONDITION_OVERDUE（每日去重），未到期不通知")
    void scanOverdue_notifiesResponsible() {
        GateElementResult overdue = conditionalRow(501L, 603L, yesterday);
        GateElementResult future = conditionalRow(501L, 603L, tomorrow);
        future.setId(9003L);
        // 查询条件本身含 leftover_status=OPEN 且 due<now：未到期行不会被查出，mock 只回逾期行
        when(resultMapper.selectList(any())).thenReturn(List.of(overdue));

        int count = service.scanOverdue(ADMIN);

        assertThat(count).isEqualTo(1);
        verify(notificationService, times(1)).publishDaily(eq(900103L),
            eq(NotificationService.Types.GATE_CONDITION_OVERDUE), eq(NotificationService.KIND_ACTION),
            eq("gate_element_results"), eq(9001L), anyString(), anyString(), anyString(), any(Date.class));
    }

    // ---------- AC-GATE-17：阻断下一 Gate ----------

    @Test
    @DisplayName("AC-GATE-17 后半：前序 G1 逾期遗留未关 ⇒ G2 提交被阻断")
    void submit_blockedByOverduePriorLegacy() {
        GateElementResult overdue = conditionalRow(501L, 603L, yesterday);
        // G2 自身要素已全判（judged 完整），阻断来自前序遗留：
        // selectList 两次调用 = submit 的 judged 查询 + requireNoOverdueLegacy 的逾期查询
        when(resultMapper.selectList(any()))
            .thenReturn(List.of(judgedRow(502L, 701L, "PASS"), judgedRow(502L, 702L, "PASS")))
            .thenReturn(List.of(overdue));

        assertThatThrownBy(() -> service.submit(502L, 9001L, 9002L, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("逾期未关闭的条件遗留");
        assertThat(g2.getStartedAt()).as("被阻断的 Gate 不应置 startedAt").isNull();
    }

    @Test
    @DisplayName("AC-GATE-17 边界：遗留已关闭或期限内未到 ⇒ 提交放行")
    void submit_passes_closedOrWithinDeadline() {
        GateElementResult closed = conditionalRow(501L, 603L, yesterday);
        closed.setLeftoverStatus("CLOSED");
        GateElementResult future = conditionalRow(501L, 603L, tomorrow);
        future.setId(9003L);
        // 逾期查询在库侧过滤 CLOSED 与未到期行 → 第二次调用返回空
        when(resultMapper.selectList(any()))
            .thenReturn(List.of(judgedRow(502L, 701L, "PASS"), judgedRow(502L, 702L, "PASS")))
            .thenReturn(List.of());

        Gate submitted = service.submit(502L, 9001L, 9002L, PM);

        assertThat(submitted.getStartedAt()).isNotNull();
    }

    // ---------- AC-GATE-17 防线：删要素不消除遗留 ----------

    @Test
    @DisplayName("AC-GATE-17 防线：要素删除/停用后遗留清单仍在、逾期标记与阻断不受影响")
    void legacy_survivesElementDeletion() {
        GateElementResult overdue = conditionalRow(501L, 603L, yesterday);
        // 要素查不到（已删）——legacyList 退化为本表留存的遗留描述
        when(elementMapper.selectById(603L)).thenReturn(null);
        // 三次 selectList：legacyList 清单 → submit judged → 逾期查询
        when(resultMapper.selectList(any()))
            .thenReturn(List.of(overdue))
            .thenReturn(List.of(judgedRow(502L, 701L, "PASS"), judgedRow(502L, 702L, "PASS")))
            .thenReturn(List.of(overdue));

        List<java.util.Map<String, Object>> legacy = service.legacyList(501L);

        assertThat(legacy).hasSize(1);
        assertThat(legacy.get(0)).containsEntry("elementCode", "(已删要素)");
        assertThat(legacy.get(0)).containsEntry("leftoverStatus", "OPEN");
        assertThat((Boolean) legacy.get(0).get("overdue")).isTrue();

        // 阻断同样不依赖要素存在（requireNoOverdueLegacy 不查要素表）
        assertThatThrownBy(() -> service.submit(502L, 9001L, 9002L, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("逾期未关闭的条件遗留");
    }
}
