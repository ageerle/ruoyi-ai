package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PersonService 跨组边界（commit bd3f3b0c / SEC-01b）场景级回归。
 *
 * <p>与 {@code PersonRehireWecomGroupLimitAcceptanceTest} 互补：该类覆盖
 * rehire/unbindWecom 的跨组拒绝、同组放行、超管旁路三主干；本类聚焦场景纵深：
 * 越权尝试零副作用、无组属组长 fail-closed、状态机分支、幂等 NOOP、审计快照脱敏与零行回滚。
 *
 * <p>缺陷 D-1（resign 未接跨组守卫）已于本类补充回归锁定：SEC-RESIGN-GROUP 守卫
 * 已落地（本人放行对齐 controller isHr-or-self 语义，其余同组/超管校验）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PersonService 组边界场景纵深（零副作用/fail-closed/状态机/幂等/审计脱敏/回滚）")
class PersonGroupBoundarySecurityScenarioTest {

    @Mock
    private PersonMapper personMapper;
    @Mock
    private ProjectMemberMapper memberMapper;
    @Mock
    private AuditLogService auditLogService;

    private PersonService service;

    private IpdActor leaderA;
    private IpdActor leaderB;
    private IpdActor superAdmin;

    @BeforeEach
    void setUp() {
        service = new PersonService(personMapper, memberMapper, auditLogService);
        leaderA = new IpdActor(101L, "Leader-A", "GROUP_LEADER", 10L);
        leaderB = new IpdActor(102L, "Leader-B", "GROUP_LEADER", 20L);
        superAdmin = new IpdActor(999L, "Root", "SUPER_ADMIN", 99L);
        lenient().when(memberMapper.selectCount(any())).thenReturn(0L);
        lenient().when(personMapper.updateById(any(Person.class))).thenReturn(1);
    }

    private Person resignedPerson(Long id, Long groupId) {
        return Person.builder()
            .id(id).name("P-" + id).groupId(groupId)
            .employmentStatus(PersonService.EM_RESIGNED)
            .accountStatus(PersonService.AC_FROZEN)
            .username("u_" + id).delFlag("0")
            .build();
    }

    private void stubPerson(Person p) {
        when(personMapper.selectById(p.getId())).thenReturn(p);
    }

    @Test
    @DisplayName("同组复职放行：写库 + REHIRE 审计快照最小化（不含 passwordHash 字样）")
    void rehire_sameGroup_writesMinimalAuditSnapshot() {
        Person p = resignedPerson(1L, 10L);
        stubPerson(p);

        service.rehire(1L, "复职说明", leaderA);

        verify(personMapper).updateById(any(Person.class));
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(captor.capture());
        AuditLog audit = captor.getValue();
        assertThat(audit.getAction()).isEqualTo("REHIRE");
        assertThat(audit.getEntityId()).isEqualTo(1L);
        assertThat(audit.getOperatorId()).isEqualTo(101L);
        assertThat(audit.getBeforeData()).contains("emp=RESIGNED", "acc=FROZEN_PENDING_HANDOVER");
        assertThat(audit.getAfterData()).contains("emp=ACTIVE", "acc=ACTIVE");
        assertThat(audit.getBeforeData() + audit.getAfterData())
            .as("审计快照不携带凭据字段")
            .doesNotContain("passwordHash");
    }

    @Test
    @DisplayName("跨组复职拒绝：人员状态字段原样、零写库、零审计（越权尝试零副作用）")
    void rehire_crossGroup_zeroSideEffects() {
        Person p = resignedPerson(2L, 10L);
        stubPerson(p);

        assertThatThrownBy(() -> service.rehire(2L, "note", leaderB))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("GROUP_LEADER 仅可复职本组员工")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);

        assertThat(p.getEmploymentStatus()).as("雇佣状态未被改动").isEqualTo(PersonService.EM_RESIGNED);
        assertThat(p.getAccountStatus()).as("账户状态未被改动").isEqualTo(PersonService.AC_FROZEN);
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("无组属组长 fail-closed：operator.groupId=null 时即使员工组也 null 仍拒绝（不做 null 等于 null 放行）")
    void leader_withoutGroup_failClosed_evenWhenPersonGroupAlsoNull() {
        IpdActor leaderNoGroup = new IpdActor(103L, "Leader-X", "GROUP_LEADER", null);
        Person p = resignedPerson(3L, null);
        stubPerson(p);

        assertThatThrownBy(() -> service.rehire(3L, null, leaderNoGroup))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
        verify(personMapper, never()).updateById(any(Person.class));
    }

    @Test
    @DisplayName("状态机：ACTIVE 员工拒绝复职（STATE_CONFLICT），DISABLED 员工拒绝复职（需先解禁）")
    void rehire_stateMachine_activeOrDisabled_rejected() {
        Person active = resignedPerson(4L, 10L);
        active.setEmploymentStatus(PersonService.EM_ACTIVE);
        active.setAccountStatus(PersonService.AC_ACTIVE);
        stubPerson(active);

        assertThatThrownBy(() -> service.rehire(4L, null, leaderA))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("当前雇佣状态不允许复职")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);

        Person disabled = resignedPerson(5L, 10L);
        disabled.setAccountStatus(PersonService.AC_DISABLED);
        stubPerson(disabled);

        assertThatThrownBy(() -> service.rehire(5L, null, leaderA))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("账户已禁用")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("已 RESIGNED 再发起离职：幂等 NOOP（零写库、零审计、返待移交数）")
    void resign_alreadyResigned_idempotentNoop() {
        Person p = resignedPerson(6L, 10L);
        stubPerson(p);

        PersonService.ResignResult result = service.resign(6L, "重复离职", leaderA);

        assertThat(result.idempotent()).as("幂等命中").isTrue();
        assertThat(result.pendingProjects()).isZero();
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("FROZEN/DISABLED 账户发起离职：STATE_CONFLICT（离职须在 ACTIVE 发起）")
    void resign_frozenOrDisabled_stateConflict() {
        Person frozen = resignedPerson(7L, 10L);
        frozen.setEmploymentStatus(PersonService.EM_ACTIVE);
        frozen.setAccountStatus(PersonService.AC_FROZEN);
        stubPerson(frozen);

        assertThatThrownBy(() -> service.resign(7L, "r", leaderA))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("当前账户状态不允许离职冻结")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);

        Person disabled = resignedPerson(8L, 10L);
        disabled.setEmploymentStatus(PersonService.EM_ACTIVE);
        disabled.setAccountStatus(PersonService.AC_DISABLED);
        stubPerson(disabled);

        assertThatThrownBy(() -> service.resign(8L, "r", leaderA))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("企微解绑成功路径：审计 before/after 均以掩码呈现，明文 wecomUserId 不入审计；写库清空并置 DISABLED")
    void unbindWecom_auditPayload_masksPlaintextWecomId() {
        Person p = resignedPerson(9L, 10L);
        p.setWecomUserId("wx-plain-8888");
        p.setWecomBoundAt(new Date());
        stubPerson(p);

        service.unbindWecom(9L, "离职解绑", leaderA);

        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personMapper).updateById(personCaptor.capture());
        assertThat(personCaptor.getValue().getWecomUserId()).as("库内企微 ID 清空").isNull();
        assertThat(personCaptor.getValue().getAccountStatus()).as("联动禁用登录").isEqualTo(PersonService.AC_DISABLED);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(captor.capture());
        AuditLog audit = captor.getValue();
        assertThat(audit.getAction()).isEqualTo("WECOM_UNBIND");
        String both = String.valueOf(audit.getBeforeData()) + audit.getAfterData();
        assertThat(both).as("审计出现掩码标记").contains("***");
        assertThat(both).as("审计不得出现明文企微 ID").doesNotContain("wx-plain-8888");
    }

    @Test
    @DisplayName("解绑幂等 NOOP：wecom_user_id 已空时直接返回（零写库、零审计）")
    void unbindWecom_blankWecom_idempotentNoop() {
        Person p = resignedPerson(10L, 10L);
        p.setWecomUserId(null);
        stubPerson(p);

        Person result = service.unbindWecom(10L, "r", leaderA);

        assertThat(result).as("幂等返回原快照").isSameAs(p);
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("updateById 零行：复职抛 ServiceException（唯一记录守卫，回滚语义）")
    void rehire_zeroRowsFromUpdate_throwsServiceException() {
        Person p = resignedPerson(11L, 10L);
        stubPerson(p);
        when(personMapper.updateById(any(Person.class))).thenReturn(0);

        assertThatThrownBy(() -> service.rehire(11L, null, leaderA))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("复职未更新唯一记录");
        verify(auditLogService, never()).append(any());
    }

    // ---- SEC-RESIGN-GROUP：resign 跨组守卫（缺陷 D-1 回归锁定） ----

    private Person activePerson(Long id, Long groupId) {
        return Person.builder()
            .id(id).name("P-" + id).groupId(groupId)
            .employmentStatus(PersonService.EM_ACTIVE)
            .accountStatus(PersonService.AC_ACTIVE)
            .username("u_" + id).delFlag("0")
            .build();
    }

    @Test
    @DisplayName("SEC-RESIGN-GROUP：跨组离职冻结拒绝（状态原样、零写库、零审计）")
    void resign_crossGroup_zeroSideEffects() {
        Person p = activePerson(21L, 10L);
        stubPerson(p);

        assertThatThrownBy(() -> service.resign(21L, "试用期", leaderB))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("GROUP_LEADER 仅可离职冻结本组员工")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);

        assertThat(p.getEmploymentStatus()).as("雇佣状态未被改动").isEqualTo(PersonService.EM_ACTIVE);
        assertThat(p.getAccountStatus()).as("账户状态未被改动").isEqualTo(PersonService.AC_ACTIVE);
        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("SEC-RESIGN-GROUP：本人离职放行（self 免组校验，对齐 controller isHr-or-self）")
    void resign_self_bypassesGroupGuard() {
        // 数据异常场景：组长本人记录挂在别的组（id 与 operator.id 相同即放行）
        Person self = activePerson(102L, 10L);
        stubPerson(self);

        PersonService.ResignResult result = service.resign(102L, "个人原因", leaderB);

        assertThat(result.message()).contains("冻结成功");
        assertThat(self.getEmploymentStatus()).isEqualTo(PersonService.EM_RESIGNED);
        assertThat(self.getAccountStatus()).isEqualTo(PersonService.AC_FROZEN);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("RESIGN");
        assertThat(captor.getValue().getOperatorId()).isEqualTo(102L);
    }

    @Test
    @DisplayName("SEC-RESIGN-GROUP：同组组长离职放行 + RESIGN 审计；超管跨组旁路放行")
    void resign_sameGroupLeader_and_superAdminCrossGroup_allowed() {
        Person member = activePerson(22L, 10L);
        stubPerson(member);
        PersonService.ResignResult result = service.resign(22L, "合同到期", leaderA);
        assertThat(result.message()).contains("冻结成功");
        verify(personMapper).updateById(any(Person.class));
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("RESIGN");

        // 超管不受组属限制
        Person other = activePerson(23L, 30L);
        stubPerson(other);
        assertThat(service.resign(23L, "异地调动", superAdmin).message()).contains("冻结成功");
        verify(personMapper, times(2)).updateById(any(Person.class));
    }
}
