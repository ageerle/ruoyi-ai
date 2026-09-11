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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-4.1 成员绑定时评级和津贴基准快照验收（BR-INC-02）。
 *
 * <p>正反例口径：
 * <ul>
 *   <li>AC-INC-02：L2 接手项目 → lockedLevel=L2 / lockedAmount=1500（allowance.L2 激活值）</li>
 *   <li>AC-HR-03：绑定后 HR 把 person.level 升为 L3 → 既有绑定快照不变（不追溯旧项目）</li>
 *   <li>AC-TEAM-10：市场PM（personType=MARKET_PM）被绑为研发PM ⇒ 拒绝（角色互斥 B7）</li>
 *   <li>重试不多成员：重复绑定拒绝且不插第二条</li>
 *   <li>涉钱取数防御：等级未同步 / allowance 参数缺失 ⇒ 拒绝</li>
 *   <li>审计：MEMBER_BIND 写 afterData 快照</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P241AcceptanceTest {

    @Mock
    private ProjectMemberMapper memberMapper;
    @Mock
    private PersonMapper personMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;

    private ProjectMemberService service;

    private static final IpdActor MARKET_LEAD =
        new IpdActor(900L, "市场PM发起人", "MARKET_PM", 7L);

    @BeforeAll
    static void initMybatisMeta() {
        // LambdaQueryWrapper 解析 ProjectMember 字段需要 lambda cache（P062 模式）
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P241-member"), ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        service = new ProjectMemberService(memberMapper, personMapper, projectMapper,
            systemConfigService, auditLogService);
        lenient().when(projectMapper.selectById(anyLong())).thenReturn(new Project());
        lenient().when(memberMapper.selectCount(any())).thenReturn(0L);
        // P2-4.2 叠加适配：bindMember 新增 allowance.projectCountThreshold 查询（活跃计数 0 时阈值分支不触发）
        lenient().when(systemConfigService.getIntValue("allowance.projectCountThreshold", 3)).thenReturn(3);
    }

    private Person pm(String personType, String level) {
        Person p = new Person();
        p.setId(201L);
        p.setName("测试" + personType);
        p.setPersonType(personType);
        p.setLevel(level);
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        p.setDelFlag("0");
        return p;
    }

    @Test
    @DisplayName("AC-INC-02 正例：L2 成员绑定 ⇒ lockedLevel=L2 / lockedAmount=1500 原子写入")
    void bind_locksSnapshot() {
        when(personMapper.selectById(201L)).thenReturn(pm("RD_PM", "L2"));
        when(systemConfigService.getIntValue("allowance.L2", -1)).thenReturn(1500);

        ProjectMember member = service.bindMember(11L, 201L, "RD_PM", MARKET_LEAD);

        assertThat(member.getLockedLevel()).isEqualTo("L2");
        assertThat(member.getLockedAmount().toPlainString()).isEqualTo("1500");
        assertThat(member.getJoinDate()).isNotNull();
        assertThat(member.getBonusEligible()).isEqualTo("1");
        verify(memberMapper).insert(member);
    }

    @Test
    @DisplayName("AC-HR-03 / AC-INC-02 反向：绑定后等级升 L3 ⇒ 既有快照仍按 L2=1500（不追溯）")
    void bind_snapshotNotAffectedByLaterLevelChange() {
        Person person = pm("RD_PM", "L2");
        when(personMapper.selectById(201L)).thenReturn(person);
        when(systemConfigService.getIntValue("allowance.L2", -1)).thenReturn(1500);

        ProjectMember bound = service.bindMember(11L, 201L, "RD_PM", MARKET_LEAD);
        // 绑定后 HR 同步把等级升为 L3（之后新绑定才按 L3 取数）
        person.setLevel("L3");

        assertThat(bound.getLockedLevel()).isEqualTo("L2");
        assertThat(bound.getLockedAmount().toPlainString()).isEqualTo("1500");
    }

    @Test
    @DisplayName("AC-TEAM-10：市场PM 被绑为研发PM ⇒ 拒绝（角色互斥 B7）")
    void bind_roleMismatch_rejected() {
        when(personMapper.selectById(201L)).thenReturn(pm("MARKET_PM", "L2"));

        assertThatThrownBy(() -> service.bindMember(11L, 201L, "RD_PM", MARKET_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("角色互斥");
        verify(memberMapper, never()).insert(any(ProjectMember.class));
    }

    @Test
    @DisplayName("重试不多成员：重复绑定 ⇒ 拒绝且不插第二条")
    void bind_duplicate_rejected() {
        when(personMapper.selectById(201L)).thenReturn(pm("RD_PM", "L2"));
        when(memberMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.bindMember(11L, 201L, "RD_PM", MARKET_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不产生重复成员");
        verify(memberMapper, never()).insert(any(ProjectMember.class));
    }

    @Test
    @DisplayName("涉钱防御：等级未同步（level 空）⇒ 拒绝绑定")
    void bind_levelMissing_rejected() {
        when(personMapper.selectById(201L)).thenReturn(pm("RD_PM", null));

        assertThatThrownBy(() -> service.bindMember(11L, 201L, "RD_PM", MARKET_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("等级未同步");
    }

    @Test
    @DisplayName("涉钱防御：allowance 参数缺失 ⇒ 拒绝绑定（不留静默默认）")
    void bind_allowanceMissing_rejected() {
        when(personMapper.selectById(201L)).thenReturn(pm("RD_PM", "L9"));

        assertThatThrownBy(() -> service.bindMember(11L, 201L, "RD_PM", MARKET_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("津贴参数缺失");
    }

    @Test
    @DisplayName("离职/禁用人员入组 ⇒ 拒绝")
    void bind_resigned_rejected() {
        Person resigned = pm("RD_PM", "L2");
        resigned.setEmploymentStatus("RESIGNED");
        when(personMapper.selectById(201L)).thenReturn(resigned);

        assertThatThrownBy(() -> service.bindMember(11L, 201L, "RD_PM", MARKET_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不可入组");
    }

    @Test
    @DisplayName("审计可追溯：MEMBER_BIND 审计行含快照 afterData")
    void bind_writesAuditWithSnapshot() {
        when(personMapper.selectById(201L)).thenReturn(pm("RD_PM", "L2"));
        when(systemConfigService.getIntValue("allowance.L2", -1)).thenReturn(1500);

        service.bindMember(11L, 201L, "RD_PM", MARKET_LEAD);

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(cap.capture());
        AuditLog log = cap.getValue();
        assertThat(log.getAction()).isEqualTo("MEMBER_BIND");
        assertThat(log.getEntityType()).isEqualTo("project_members");
        assertThat(log.getOperatorId()).isEqualTo(900L);
        assertThat(log.getAfterData()).contains("L2").contains("1500");
    }

    @Test
    @DisplayName("列表：仅返回在组成员（未退出）")
    void list_activeMembers() {
        ProjectMember m = ProjectMember.builder().projectId(11L).personId(201L).role("RD_PM")
            .lockedLevel("L2").lockedAmount(new java.math.BigDecimal("1500")).build();
        when(memberMapper.selectList(any())).thenReturn(List.of(m));

        List<ProjectMember> members = service.listActiveMembers(11L);

        assertThat(members).hasSize(1);
        assertThat(members.get(0).getLockedAmount().toPlainString()).isEqualTo("1500");
    }
}
