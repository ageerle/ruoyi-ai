package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * P2-1.3 人员账户状态联动验收测试（AC-USER-08/09/10；BR-USER-05/06）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>resign: 正常冻结 / 已 RESIGNED 幂等 / FROZEN 状态拒绝 / DISABLED 状态拒绝 / 审计写入</li>
 *   <li>rehire: RESIGNED→ACTIVE / ACTIVE 拒绝 / DISABLED 拒绝 / 审计写入</li>
 *   <li>unbindWecom: 清空 wecom + DISABLED / 已空幂等 / 审计写入</li>
 *   <li>不存在的 personId 报 NOT_FOUND</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P2-1.3 人员账户状态联动")
class P213PersonStateAcceptanceTest {

    @Mock PersonMapper personMapper;
    @Mock ProjectMemberMapper memberMapper;
    @Mock AuditLogService auditLogService;

    @InjectMocks PersonService personService;

    private IpdActor hrActor;
    private IpdActor selfActor;

    @BeforeEach
    void setUp() {
        hrActor = new IpdActor(100L, "Alice-HR", "GROUP_LEADER", 10L);
        selfActor = new IpdActor(200L, "Bob-Self", "MARKET_PM", 10L);
        // 不在 setUp 默认 stub memberMapper.selectCount：避免覆盖测试内的 .thenReturn().thenReturn() 链
        // 需要返回 0L 默认的测试请自行 stub（lenient）
        // 默认 updateById 返 1（成功路径需要；默认 Mockito 返 0 会触发 rows != 1 ServiceException）
        lenient().when(personMapper.updateById(any(org.ruoyi.ipd.domain.Person.class))).thenReturn(1);
    }

    private Person person(Long id, String emp, String acc, String wecom) {
        return Person.builder()
            .id(id)
            .name("Test-" + id)
            .employeeNo("E" + id)
            .personType("MARKET_PM")
            .groupId(10L)
            .level("L3")
            .employmentStatus(emp)
            .accountStatus(acc)
            .wecomUserId(wecom)
            .username("u" + id)
            .delFlag("0")
            .build();
    }

    @Test
    @DisplayName("resign: ACTIVE → RESIGNED + FROZEN + 审计 + 返回 pendingProjects=0")
    void resign_active_freezes() {
        Person p = person(1L, "ACTIVE", "ACTIVE", "wc_001");
        when(personMapper.selectById(1L)).thenReturn(p);

        var result = personService.resign(1L, "personal reason", hrActor);

        assertThat(result.idempotent()).isFalse();
        assertThat(result.pendingProjects()).isZero();
        assertThat(result.message()).contains("冻结成功");
        ArgumentCaptor<Person> saved = ArgumentCaptor.forClass(Person.class);
        verify(personMapper).updateById(saved.capture());
        assertThat(saved.getValue().getEmploymentStatus()).isEqualTo("RESIGNED");
        assertThat(saved.getValue().getAccountStatus()).isEqualTo("FROZEN_PENDING_HANDOVER");

        ArgumentCaptor<AuditLog> audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(audit.capture());
        assertThat(audit.getValue().getAction()).isEqualTo("RESIGN");
        assertThat(audit.getValue().getEntityType()).isEqualTo("persons");
        assertThat(audit.getValue().getOperatorId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("resign: 幂等命中（已 RESIGNED 返 idempotent=true + 不重复写）")
    void resign_idempotent_whenAlreadyResigned() {
        Person p = person(2L, "RESIGNED", "FROZEN_PENDING_HANDOVER", null);
        when(personMapper.selectById(2L)).thenReturn(p);

        var result = personService.resign(2L, "再次调用", hrActor);

        assertThat(result.idempotent()).isTrue();
        verify(personMapper, never()).updateById(any(org.ruoyi.ipd.domain.Person.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("resign: FROZEN_PENDING_HANDOVER 状态拒绝（避免重复冻结）")
    void resign_rejectsFrozenState() {
        Person p = person(3L, "ACTIVE", "FROZEN_PENDING_HANDOVER", null);
        when(personMapper.selectById(3L)).thenReturn(p);

        assertThatThrownBy(() -> personService.resign(3L, "x", hrActor))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("FROZEN_PENDING_HANDOVER");
    }

    @Test
    @DisplayName("resign: DISABLED 状态拒绝")
    void resign_rejectsDisabledState() {
        Person p = person(4L, "ACTIVE", "DISABLED", null);
        when(personMapper.selectById(4L)).thenReturn(p);

        assertThatThrownBy(() -> personService.resign(4L, "x", hrActor))
            .isInstanceOf(IpdBusinessException.class);
    }

    @Test
    @DisplayName("rehire: RESIGNED → ACTIVE + 审计")
    void rehire_resumesToActive() {
        Person p = person(5L, "RESIGNED", "FROZEN_PENDING_HANDOVER", null);
        when(personMapper.selectById(5L)).thenReturn(p);

        Person result = personService.rehire(5L, "back to work", hrActor);

        assertThat(result.getEmploymentStatus()).isEqualTo("ACTIVE");
        assertThat(result.getAccountStatus()).isEqualTo("ACTIVE");
        ArgumentCaptor<AuditLog> audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(audit.capture());
        assertThat(audit.getValue().getAction()).isEqualTo("REHIRE");
    }

    @Test
    @DisplayName("rehire: ACTIVE 状态拒绝（不能重复复职）")
    void rehire_rejectsAlreadyActive() {
        Person p = person(6L, "ACTIVE", "ACTIVE", "wc_002");
        when(personMapper.selectById(6L)).thenReturn(p);

        assertThatThrownBy(() -> personService.rehire(6L, "x", hrActor))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("ACTIVE");
    }

    @Test
    @DisplayName("rehire: DISABLED 拒绝（需先解禁）")
    void rehire_rejectsDisabledAccount() {
        Person p = person(7L, "RESIGNED", "DISABLED", null);
        when(personMapper.selectById(7L)).thenReturn(p);

        assertThatThrownBy(() -> personService.rehire(7L, "x", hrActor))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("禁用");  // pre-existing: 中文错误文案（不改 PersonService 文案避免契约漂移）
    }

    @Test
    @DisplayName("unbindWecom: 清空 wecom_user_id + account→DISABLED + 审计（RESIGNED 员工；ACTIVE 走新守卫另测）")
    void unbindWecom_clearsAndDisables() {
        // SEC-02b/MEDIUM 新契约：在职员工解绑企微需 SUPER_ADMIN 或先离职冻结；
        // 此测覆盖 RESIGNED 员工正常解绑联动路径。
        Person p = person(8L, "RESIGNED", "FROZEN_PENDING_HANDOVER", "wc_999");
        when(personMapper.selectById(8L)).thenReturn(p);

        Person result = personService.unbindWecom(8L, "resigned wecom", hrActor);

        assertThat(result.getWecomUserId()).isNull();
        assertThat(result.getAccountStatus()).isEqualTo("DISABLED");
        ArgumentCaptor<AuditLog> audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(audit.capture());
        assertThat(audit.getValue().getAction()).isEqualTo("WECOM_UNBIND");
        assertThat(audit.getValue().getBeforeData()).contains("wecom=***");  // snapshot() 屏蔽实际值；pre-existing 测断言 wc_999 错
    }

    @Test
    @DisplayName("unbindWecom: 幂等命中（wecom 已空 返原 person + 不重复写；RESIGNED 路径）")
    void unbindWecom_idempotent_whenAlreadyEmpty() {
        // SEC-02b/MEDIUM：RESIGNED 员工可解绑（wecom 已空返 NOOP）
        Person p = person(9L, "RESIGNED", "FROZEN_PENDING_HANDOVER", null);
        when(personMapper.selectById(9L)).thenReturn(p);

        Person result = personService.unbindWecom(9L, "再解绑一次", hrActor);

        assertThat(result).isSameAs(p);
        verify(personMapper, never()).updateById(any(org.ruoyi.ipd.domain.Person.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("人员不存在返 NOT_FOUND（resign/rehire/unbind 三端点统一）")
    void personNotFound_throwsForAllThreeOps() {
        when(personMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> personService.resign(99L, "x", hrActor))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);

        assertThatThrownBy(() -> personService.rehire(99L, "x", hrActor))
            .isInstanceOf(IpdBusinessException.class);
        assertThatThrownBy(() -> personService.unbindWecom(99L, "x", hrActor))
            .isInstanceOf(IpdBusinessException.class);
    }

    @Test
    @DisplayName("resign: pendingProjects 反映当前 active membership 数（带项目）")
    void resign_pendingProjectsReflectsMemberships() {
        Person p = person(11L, "ACTIVE", "ACTIVE", "wc_x");
        when(personMapper.selectById(11L)).thenReturn(p);
        // resign 只在写入成功后调一次 selectCount（不存在幂等检查调用，ACTIVE 路径直接走 update）
        when(memberMapper.selectCount(any())).thenReturn(2L);

        var result = personService.resign(11L, "team restructure", hrActor);

        assertThat(result.pendingProjects()).isEqualTo(2L);
    }

    @Test
    @DisplayName("resign: updateById 影响行数 ≠ 1 抛 ServiceException（数据漂移守卫）")
    void resign_updateAffectsNonOneRow_throwsServiceException() {
        Person p = person(12L, "ACTIVE", "ACTIVE", "wc_y");
        when(personMapper.selectById(12L)).thenReturn(p);
        when(personMapper.updateById(any(org.ruoyi.ipd.domain.Person.class))).thenReturn(0);

        assertThatThrownBy(() -> personService.resign(12L, "x", hrActor))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未更新唯一记录");
    }
}
