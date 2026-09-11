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
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateReviewObserver;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.GateArbitrationMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.GateReviewMapper;
import org.ruoyi.ipd.mapper.GateReviewObserverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MEDIUM-1.3 Gate 列席人员邀请 5 测验收：
 * <ol>
 *   <li>超管邀请 3 个列席 → 3 行 observer</li>
 *   <li>重复邀请同一 observer → 幂等</li>
 *   <li>列席人提交意见 → audit + opinion 落库</li>
 *   <li>列席 A 替 B 提交意见 → 403</li>
 *   <li>查询 1 个 gate 列席列表 + opinion 字段</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class GateReviewObserverAcceptanceTest {

    @Mock
    private GateMapper gateMapper;
    @Mock
    private GateReviewMapper reviewMapper;
    @Mock
    private ProjectMemberMapper memberMapper;
    @Mock
    private PersonMapper personMapper;
    @Mock
    private GateArbitrationMapper arbitrationMapper;
    @Mock
    private GateReviewObserverMapper observerMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private GateReviewService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Person.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
        TableInfoHelper.initTableInfo(assistant, GateReviewObserver.class);
    }

    @BeforeEach
    void setUp() {
        service = new GateReviewService(gateMapper, reviewMapper, memberMapper, personMapper,
            arbitrationMapper, observerMapper, systemConfigService, auditLogService, notificationService);
    }

    private IpdActor adminActor() {
        return new IpdActor(1L, "super-admin", "SUPER_ADMIN", null);
    }

    private IpdActor leaderActor() {
        return new IpdActor(7L, "leader", "GROUP_LEADER", 7L);
    }

    private IpdActor salesActor(long id) {
        return new IpdActor(id, "sales-" + id, "MARKET_PM", 7L);
    }

    private Gate pendingGate() {
        return Gate.builder().id(100L).projectId(1L).gateCode("G1")
            .status("PENDING").currentRound(1).build();
    }

    private Person person(long id, String type) {
        Person p = new Person();
        p.setId(id);
        p.setName(type + "-" + id);
        p.setPersonType(type);
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        return p;
    }

    @SuppressWarnings("unchecked")
    private void mockObserverCount(long gateId, long observerId, long count) {
        when(observerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(count);
    }

    private void mockObserverExists(long gateId, long observerId, GateReviewObserver row) {
        when(observerMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(row);
    }

    @SuppressWarnings("unchecked")
    private void mockObserverList(long gateId, List<GateReviewObserver> rows) {
        when(observerMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rows);
    }

    @Test
    @DisplayName("MEDIUM-1.3-1：超管邀请 3 个列席 → observerMapper.insert 3 次 + audit + 3 通知")
    void adminInviteThreeObservers() {
        when(gateMapper.selectById(100L)).thenReturn(pendingGate());
        when(personMapper.selectById(11L)).thenReturn(person(11L, "MARKET_PM"));
        when(personMapper.selectById(12L)).thenReturn(person(12L, "MARKET_PM"));
        when(personMapper.selectById(13L)).thenReturn(person(13L, "MARKET_PM"));
        // 3 个 observer 都不存在 → count=0
        when(observerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        int count = service.inviteObservers(100L, List.of(11L, 12L, 13L), "SALES", adminActor());

        assertThat(count).isEqualTo(3);
        verify(observerMapper, times(3)).insert(any(GateReviewObserver.class));
        verify(notificationService, times(3))
            .publish(any(), any(), any(), any(), any(), any(), any(), any());

        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .contains("GATE_OBSERVER_INVITED");
    }

    @Test
    @DisplayName("MEDIUM-1.3-2：重复邀请同一 observer → 幂等不重复插行")
    void inviteDuplicateObserverIsIdempotent() {
        when(gateMapper.selectById(100L)).thenReturn(pendingGate());
        when(personMapper.selectById(11L)).thenReturn(person(11L, "MARKET_PM"));
        // 存在 → count=1 → 不调 insert
        when(observerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        int count = service.inviteObservers(100L, List.of(11L), "SALES", adminActor());

        assertThat(count).isZero();
        verify(observerMapper, never()).insert(any(GateReviewObserver.class));
        // 仍写一条 audit 记录（用于追溯"重复邀请"行为）
        verify(auditLogService, atLeastOnce()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("MEDIUM-1.3-3：列席人本人提交意见 → opinion 落库 + audit OBSERVER_OPINION")
    void observerRecordOpinionSuccess() {
        when(gateMapper.selectById(100L)).thenReturn(pendingGate());
        GateReviewObserver row = GateReviewObserver.builder()
            .id(1L).gateId(100L).observerId(11L).role("SALES")
            .invitedBy(1L).attended(0).build();
        mockObserverExists(100L, 11L, row);

        IpdActor sales = salesActor(11L);
        GateReviewObserver updated = service.recordOpinion(100L, 11L,
            "客户群体扩展建议", sales);

        assertThat(updated.getOpinion()).isEqualTo("客户群体扩展建议");
        assertThat(updated.getAttended()).isEqualTo(1);
        verify(observerMapper, atLeastOnce()).updateById(any(GateReviewObserver.class));

        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .contains("GATE_OBSERVER_OPINION");
    }

    @Test
    @DisplayName("MEDIUM-1.3-4：列席 A 替 B 提交意见 → 403（横向越权防护）")
    void observerCannotSubmitForAnother() {
        when(gateMapper.selectById(100L)).thenReturn(pendingGate());
        IpdActor salesA = salesActor(11L);  // 想替 12L 写意见

        assertThatThrownBy(() -> service.recordOpinion(100L, 12L, "假冒意见", salesA))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅本人");

        verify(observerMapper, never()).updateById(any(GateReviewObserver.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("MEDIUM-1.3-5：超管/组长查询 Gate 列席列表 → opinion 字段全揭示")
    void listObserversRevealsOpinion() {
        when(gateMapper.selectById(100L)).thenReturn(pendingGate());
        GateReviewObserver o1 = GateReviewObserver.builder()
            .id(1L).gateId(100L).observerId(11L).role("SALES")
            .invitedBy(1L).attended(1).opinion("客户群体扩展").build();
        GateReviewObserver o2 = GateReviewObserver.builder()
            .id(2L).gateId(100L).observerId(12L).role("SUPPLY")
            .invitedBy(1L).attended(0).opinion(null).build();
        mockObserverList(100L, List.of(o1, o2));

        // 组长查
        List<GateReviewObserver> rows = service.listObservers(100L, leaderActor());
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getOpinion()).isEqualTo("客户群体扩展");
        assertThat(rows.get(1).getOpinion()).isNull();

        // 超管查
        List<GateReviewObserver> rows2 = service.listObservers(100L, adminActor());
        assertThat(rows2).hasSize(2);
    }

    @Test
    @DisplayName("MEDIUM-1.3-补充：MARKET_PM 不能邀请列席（非超管/组长） → 403")
    void marketPmCannotInviteObservers() {
        when(gateMapper.selectById(100L)).thenReturn(pendingGate());
        IpdActor pm = salesActor(11L);
        assertThatThrownBy(() ->
            service.inviteObservers(100L, List.of(99L), "SALES", pm))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("超管/产品组长");
        verify(observerMapper, never()).insert(any(GateReviewObserver.class));
    }

    @Test
    @DisplayName("MEDIUM-1.3-补充：MARKET_PM 不能查 listObservers（仅组长/超管可见） → 403")
    void marketPmCannotListObservers() {
        when(gateMapper.selectById(100L)).thenReturn(pendingGate());
        IpdActor pm = salesActor(11L);
        assertThatThrownBy(() -> service.listObservers(100L, pm))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("组长/超管");
    }
}