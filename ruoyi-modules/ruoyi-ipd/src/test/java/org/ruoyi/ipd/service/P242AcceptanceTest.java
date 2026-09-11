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
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-4.2 主/附加项目定义与超额备案审核验收（AC-TEAM-11；BR-ORG-03/BR-TEAM-09）。
 *
 * <p>正反例口径：
 * <ul>
 *   <li>AC-TEAM-11 反例：PM 已绑定 2 个项目，绑第 3 个未录入备案 ⇒ 禁止绑定</li>
 *   <li>AC-TEAM-11 正例：第 3 个携带评级委员会备案编号 ⇒ 允许，编号落库 + 写审计</li>
 *   <li>上限硬顶：第 4 个即使带备案也拒绝（threshold=3 上限，备案不是无限通行证）</li>
 *   <li>主/附加定义：首个活跃绑定=PRIMARY，其余=ADDITIONAL</li>
 *   <li>并发不突破：活跃计数查询带 FOR UPDATE（person_id 索引串行化），条件真实下推</li>
 *   <li>阈值配置驱动：allowance.projectCountThreshold 生效（种子 3）</li>
 *   <li>P2-4.1 兼容：4 参旧签名 = approvalRef=null 的 5 参，既有行为不回归</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P242AcceptanceTest {

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

    private static final IpdActor MARKET_LEAD = new IpdActor(900L, "市场PM发起人", "MARKET_PM", 7L);

    @BeforeAll
    static void initMybatisMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P242-member"), ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        service = new ProjectMemberService(memberMapper, personMapper, projectMapper,
            systemConfigService, auditLogService);
        lenient().when(projectMapper.selectById(anyLong())).thenReturn(new Project());
        lenient().when(systemConfigService.getIntValue("allowance.projectCountThreshold", 3)).thenReturn(3);
        lenient().when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Person rdPm() {
        Person p = new Person();
        p.setId(201L);
        p.setName("测试研发PM");
        p.setPersonType("RD_PM");
        p.setLevel("L2");
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        p.setDelFlag("0");
        return p;
    }

    /** 幂等检查（无 FOR UPDATE 段）恒 0；活跃计数（带 FOR UPDATE）按场景给值——按语义区分，任意次数调用稳定。 */
    private void activeBindsIs(long activeCount) {
        when(memberMapper.selectCount(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            String seg = w.getSqlSegment();
            return seg != null && seg.contains("FOR UPDATE") ? activeCount : 0L;
        });
    }

    private void stubAllowance() {
        lenient().when(personMapper.selectById(201L)).thenReturn(rdPm());
        lenient().when(systemConfigService.getIntValue("allowance.L2", -1)).thenReturn(1500);
    }

    @Test
    @DisplayName("主/附加定义：首个活跃绑定 ⇒ PRIMARY 且无需备案")
    void firstBindIsPrimary() {
        stubAllowance();
        activeBindsIs(0L);
        ProjectMember m = service.bindMember(11L, 201L, "RD_PM", null, MARKET_LEAD);
        assertThat(m.getMemberType()).isEqualTo("PRIMARY");
        assertThat(m.getApprovalRef()).isNull();
    }

    @Test
    @DisplayName("第 2 个绑定 ⇒ ADDITIONAL，仍无需备案")
    void secondBindIsAdditional() {
        stubAllowance();
        activeBindsIs(1L);
        ProjectMember m = service.bindMember(12L, 201L, "RD_PM", null, MARKET_LEAD);
        assertThat(m.getMemberType()).isEqualTo("ADDITIONAL");
        assertThat(m.getApprovalRef()).isNull();
    }

    @Test
    @DisplayName("AC-TEAM-11 反例：已绑 2 个，第 3 个未录备案 ⇒ 禁止绑定")
    void thirdBindWithoutApprovalRejected() {
        stubAllowance();
        activeBindsIs(2L);
        assertThatThrownBy(() -> service.bindMember(13L, 201L, "RD_PM", null, MARKET_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("备案");
        verify(memberMapper, never()).insert(any(ProjectMember.class));
    }

    @Test
    @DisplayName("AC-TEAM-11 正例：第 3 个携带备案编号 ⇒ 允许；编号 trim 落库 + 审计含备案与 memberType")
    void thirdBindWithApprovalSucceedsAndAudited() {
        stubAllowance();
        activeBindsIs(2L);
        ProjectMember m = service.bindMember(13L, 201L, "RD_PM", " PC-2026-0042 ", MARKET_LEAD);
        assertThat(m.getMemberType()).isEqualTo("ADDITIONAL");
        assertThat(m.getApprovalRef()).isEqualTo("PC-2026-0042");

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(cap.capture());
        AuditLog log = cap.getValue();
        assertThat(log.getAction()).isEqualTo("MEMBER_BIND");
        assertThat(log.getAfterData()).contains("PC-2026-0042");
        assertThat(log.getAfterData()).contains("ADDITIONAL");
        assertThat(log.getAfterData()).contains("\"activeCountBefore\":2");
    }

    @Test
    @DisplayName("上限硬顶：已绑 3 个，第 4 个即使带备案 ⇒ 拒绝")
    void fourthBindRejectedEvenWithApproval() {
        stubAllowance();
        activeBindsIs(3L);
        assertThatThrownBy(() -> service.bindMember(14L, 201L, "RD_PM", "PC-2026-0099", MARKET_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("上限");
        verify(memberMapper, never()).insert(any(ProjectMember.class));
    }

    @Test
    @DisplayName("并发不突破：活跃计数条件真实下推（person_id+exit_date）且带 FOR UPDATE")
    void concurrencyCountIsGuardedAndPushedDown() {
        stubAllowance();
        activeBindsIs(2L);
        service.bindMember(13L, 201L, "RD_PM", "PC-2026-0042", MARKET_LEAD);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<ProjectMember>> cap =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(memberMapper, times(2)).selectCount(cap.capture());
        String activeCountSql = cap.getAllValues().get(1).getSqlSegment();
        assertThat(activeCountSql).as("活跃计数按 person_id 维度").contains("person_id");
        assertThat(activeCountSql).as("只数未退出成员").contains("exit_date");
        assertThat(activeCountSql).as("FOR UPDATE 锁索引，并发绑定串行化").contains("FOR UPDATE");
    }

    @Test
    @DisplayName("P2-4.1 兼容：4 参旧签名 = 未备案，已绑 2 个时同样被拒（既有调用方零回归）")
    void fourParamLegacyDelegation() {
        stubAllowance();
        activeBindsIs(2L);
        assertThatThrownBy(() -> service.bindMember(13L, 201L, "RD_PM", MARKET_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("备案");
    }

    @Test
    @DisplayName("阈值配置驱动：threshold=2 时第 2 个绑定即要求备案")
    void thresholdIsConfigDriven() {
        stubAllowance();
        when(systemConfigService.getIntValue("allowance.projectCountThreshold", 3)).thenReturn(2);
        activeBindsIs(1L);
        assertThatThrownBy(() -> service.bindMember(12L, 201L, "RD_PM", null, MARKET_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("备案");
        ProjectMember m = service.bindMember(12L, 201L, "RD_PM", "PC-2026-0002", MARKET_LEAD);
        assertThat(m.getApprovalRef()).isEqualTo("PC-2026-0002");
    }

    @Test
    @DisplayName("备案编号超长拒绝（对齐 approval_ref varchar(64)，防 Data too long）")
    void overlongApprovalRefRejected() {
        stubAllowance();
        activeBindsIs(2L);
        String longRef = "A".repeat(65);
        assertThatThrownBy(() -> service.bindMember(13L, 201L, "RD_PM", longRef, MARKET_LEAD))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("64");
        verify(memberMapper, never()).insert(any(ProjectMember.class));
    }
}
