package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.CoefficientChangeRequest;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.CoefficientChangeRequestMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import org.ruoyi.ipd.security.IpdActor;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AC-INC-15c：双PM 提议 → 产品组长确认写档。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class CoefficientChangeServiceTest {

    @Mock private CoefficientChangeRequestMapper requestMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private AuditLogService auditLogService;

    private CoefficientChangeService service;

    @BeforeEach
    void setUp() {
        service = new CoefficientChangeService(requestMapper, projectMapper, auditLogService);
    }

    private IpdActor proposerActor() {
        // 提议人=市场PM 101L，归属同组（与 sProject().mainGroupId 对齐）
        return new IpdActor(101L, "M", "MARKET_PM", 7L);
    }

    private Project sProject() {
        Project p = new Project();
        p.setId(10L);
        p.setLevel("S");
        p.setLevelCoefficient(new BigDecimal("1.5"));
        p.setMainGroupId(7L); // 合流补参：同组守卫 assertSameGroupIpd 需与 proposerActor().groupId 一致
        p.setDelFlag("0");
        return p;
    }

    @Test
    @DisplayName("双PM提议进入 PENDING_LEADER；A 级拒绝；越界拒绝")
    void proposeHappyAndGuards() {
        when(projectMapper.selectById(10L)).thenReturn(sProject());
        when(requestMapper.selectCount(any())).thenReturn(0L);
        when(requestMapper.insert(any(CoefficientChangeRequest.class))).thenAnswer(inv -> {
            CoefficientChangeRequest r = inv.getArgument(0);
            r.setId(99L);
            return 1;
        });

        CoefficientChangeRequest created = service.propose(
            10L, new BigDecimal("1.8"), "旗舰溢价", 101L, 102L, 101L,
            // 合流对齐：第 7 参为操作人 actor（须为双 PM 之一或超管，R11/A2 同组守卫）
            proposerActor());
        assertThat(created.getStatus()).isEqualTo(CoefficientChangeRequest.ST_PENDING_LEADER);
        assertThat(created.getProposedCoefficient()).isEqualByComparingTo("1.8");

        Project a = sProject();
        a.setLevel("A");
        when(projectMapper.selectById(11L)).thenReturn(a);
        assertThatThrownBy(() -> service.propose(11L, new BigDecimal("1.0"), "x", 101L, 102L, 101L, proposerActor()))
            .isInstanceOf(ServiceException.class).hasMessageContaining("A 级");

        assertThatThrownBy(() -> service.propose(10L, new BigDecimal("2.5"), "越界", 101L, 102L, 101L, proposerActor()))
            .isInstanceOf(ServiceException.class).hasMessageContaining("1.5–2.0");
    }

    @Test
    @DisplayName("组长确认后写入项目 levelCoefficient")
    void leaderConfirmWritesArchive() {
        CoefficientChangeRequest pending = CoefficientChangeRequest.builder()
            .id(99L).projectId(10L).proposedCoefficient(new BigDecimal("1.8"))
            .reason("旗舰").marketPmId(1L).rdPmId(2L).proposerId(1L)
            .status(CoefficientChangeRequest.ST_PENDING_LEADER).build();
        when(requestMapper.selectById(99L)).thenReturn(pending);
        when(projectMapper.selectById(10L)).thenReturn(sProject());
        when(requestMapper.updateById(any(CoefficientChangeRequest.class))).thenReturn(1);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        CoefficientChangeRequest done = service.leaderDecision(99L, 900L, true, "同意",
            new IpdActor(900L, "L", "GROUP_LEADER", 7L));
        assertThat(done.getStatus()).isEqualTo(CoefficientChangeRequest.ST_CONFIRMED);

        ArgumentCaptor<Project> cap = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).updateById(cap.capture());
        assertThat(cap.getValue().getLevelCoefficient()).isEqualByComparingTo("1.8");
        assertThat(cap.getValue().getLevelCoefficientReason()).isEqualTo("旗舰");
    }
}
