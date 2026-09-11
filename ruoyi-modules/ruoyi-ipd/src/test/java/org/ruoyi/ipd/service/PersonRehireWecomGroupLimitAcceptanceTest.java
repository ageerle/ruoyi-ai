package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * PersonService.rehire / unbindWecom 授权审查闭环验收测试（SEC-01b/HIGH + SEC-02b/MEDIUM）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>rehire 跨组守卫：GROUP_LEADER 跨组 → FORBIDDEN / 同组放行 / SUPER_ADMIN 例外</li>
 *   <li>unbindWecom 跨组守卫：同上</li>
 *   <li>unbindWecom 在职守卫：ACTIVE 员工被解绑企微 → STATE_CONFLICT（GROUP_LEADER 触发）/ SUPER_ADMIN 例外</li>
 * </ul>
 *
 * <p>6 个测试覆盖三类守卫 × 两端点；不重不漏。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("PersonService rehire/unbindWecom 授权守卫")
class PersonRehireWecomGroupLimitAcceptanceTest {

    @Mock PersonMapper personMapper;
    @Mock ProjectMemberMapper memberMapper;
    @Mock AuditLogService auditLogService;

    @InjectMocks PersonService personService;

    private IpdActor groupALeader; // GROUP_LEADER groupId=10L
    private IpdActor groupBLeader; // GROUP_LEADER groupId=20L
    private IpdActor superAdmin;   // SUPER_ADMIN groupId=99L

    @BeforeEach
    void setUp() {
        groupALeader = new IpdActor(101L, "Leader-A", "GROUP_LEADER", 10L);
        groupBLeader = new IpdActor(102L, "Leader-B", "GROUP_LEADER", 20L);
        superAdmin   = new IpdActor(999L, "Root", "SUPER_ADMIN", 99L);
        // lenient: 部分测试不触发 selectCount（抛错前返回）
        lenient().when(memberMapper.selectCount(any())).thenReturn(0L);
        // 默认 updateById 返 1（成功路径需要；默认 Mockito 返 0 会触发 rows != 1 ServiceException）
        lenient().when(personMapper.updateById(any(Person.class))).thenReturn(1);
    }

    private Person person(Long id, String emp, String acc, String wecom, Long groupId) {
        return Person.builder()
            .id(id)
            .name("Test-" + id)
            .employeeNo("E" + id)
            .personType("MARKET_PM")
            .groupId(groupId)
            .level("L3")
            .employmentStatus(emp)
            .accountStatus(acc)
            .wecomUserId(wecom)
            .username("u" + id)
            .delFlag("0")
            .build();
    }

    // ───────────── rehire 跨组守卫 ─────────────

    @Test
    @DisplayName("rehire: GROUP_LEADER 跨组禁止（操作组≠员工组 → FORBIDDEN，不写不审）")
    void rehire_crossGroup_forbidden_GROUP_LEADER() {
        Person p = person(1L, "RESIGNED", "FROZEN_PENDING_HANDOVER", null, 20L); // 员工在 groupB
        when(personMapper.selectById(1L)).thenReturn(p);

        assertThatThrownBy(() -> personService.rehire(1L, "note", groupALeader))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);

        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("rehire: GROUP_LEADER 同组放行（操作组=员工组 → 写库 + 审计）")
    void rehire_sameGroup_allowed_GROUP_LEADER() {
        Person p = person(2L, "RESIGNED", "FROZEN_PENDING_HANDOVER", null, 10L);
        when(personMapper.selectById(2L)).thenReturn(p);

        Person result = personService.rehire(2L, "back to work", groupALeader);

        assertThat(result.getEmploymentStatus()).isEqualTo("ACTIVE");
        assertThat(result.getAccountStatus()).isEqualTo("ACTIVE");
        verify(personMapper).updateById(any(Person.class));
        verify(auditLogService).append(any());
    }

    @Test
    @DisplayName("rehire: SUPER_ADMIN 跨组例外放行（操作组=99 vs 员工组=10 → 写库）")
    void rehire_crossGroup_allowed_SUPER_ADMIN() {
        Person p = person(3L, "RESIGNED", "FROZEN_PENDING_HANDOVER", null, 10L);
        when(personMapper.selectById(3L)).thenReturn(p);

        Person result = personService.rehire(3L, "admin override", superAdmin);

        assertThat(result.getEmploymentStatus()).isEqualTo("ACTIVE");
        verify(personMapper).updateById(any(Person.class));
    }

    // ───────────── unbindWecom 跨组守卫 ─────────────

    @Test
    @DisplayName("unbindWecom: GROUP_LEADER 跨组禁止（操作组≠员工组 → FORBIDDEN）")
    void unbindWecom_crossGroup_forbidden_GROUP_LEADER() {
        Person p = person(4L, "ACTIVE", "ACTIVE", "wc_001", 20L); // 员工在 groupB
        when(personMapper.selectById(4L)).thenReturn(p);

        assertThatThrownBy(() -> personService.unbindWecom(4L, "cross-group attempt", groupALeader))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);

        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any());
    }

    // ───────────── unbindWecom 在职守卫 ─────────────

    @Test
    @DisplayName("unbindWecom: 在职员工 + 非超管 → STATE_CONFLICT（需先离职冻结）")
    void unbindWecom_activeEmployee_forbidden_nonAdmin() {
        Person p = person(5L, "ACTIVE", "ACTIVE", "wc_002", 10L); // 在职 + 本组
        when(personMapper.selectById(5L)).thenReturn(p);

        assertThatThrownBy(() -> personService.unbindWecom(5L, "should not proceed", groupALeader))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);

        verify(personMapper, never()).updateById(any(Person.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("unbindWecom: 在职员工 + SUPER_ADMIN → 例外放行（合规封禁场景）")
    void unbindWecom_activeEmployee_allowed_SUPER_ADMIN() {
        Person p = person(6L, "ACTIVE", "ACTIVE", "wc_003", 10L);
        when(personMapper.selectById(6L)).thenReturn(p);

        Person result = personService.unbindWecom(6L, "compliance block", superAdmin);

        assertThat(result.getWecomUserId()).isNull();
        assertThat(result.getAccountStatus()).isEqualTo("DISABLED");
        verify(personMapper).updateById(any(Person.class));
        verify(auditLogService).append(any());
    }
}