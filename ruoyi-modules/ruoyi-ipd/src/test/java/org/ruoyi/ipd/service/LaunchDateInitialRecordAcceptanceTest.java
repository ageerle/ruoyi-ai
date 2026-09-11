package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.LaunchDateChangeRequestMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1 / §5.1 HIGH-1.1：L08 上市日期初次录入（独立端点）。
 *
 * <p>边界条件：
 * <ol>
 *   <li>DRAFT 状态 + 空白理由 ⇒ 拒</li>
 *   <li>DRAFT 状态 + 合法理由 + launchDate=null ⇒ 成功 + 写 INITIAL_LAUNCH_DATE 审计</li>
 *   <li>CONFIRMED 状态 ⇒ 成功</li>
 *   <li>ARCHIVED 状态 ⇒ 拒</li>
 *   <li>已存在 launch_date ⇒ 拒（走双签流程）</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class LaunchDateInitialRecordAcceptanceTest {

    @Mock private LaunchDateChangeRequestMapper requestMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private AuditLogService auditLogService;

    private LaunchDateChangeService service;

    private static final Date TARGET = new Date(1_900_000_000_000L);

    @BeforeEach
    void setUp() {
        service = new LaunchDateChangeService(requestMapper, projectMapper, auditLogService);
    }

    private Project draft(Date launchDate) {
        return Project.builder().id(900L).name("L08").status("DRAFT").delFlag("0")
            .mainGroupId(70L).launchDate(launchDate).build();
    }

    @Test
    @DisplayName("AC#1 缺理由 ⇒ ServiceException")
    void missingReason_rejected() {
        assertThatThrownBy(() -> service.initialRecord(900L, TARGET, "", 800L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("理由");
        verify(projectMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("AC#2 DRAFT + launchDate=null + 合法理由 ⇒ 成功 + 写 INITIAL_LAUNCH_DATE 审计")
    void draftFirstTime_succeeds() {
        Project p = draft(null);
        when(projectMapper.selectById(900L)).thenReturn(p);
        Project result = service.initialRecord(900L, TARGET, "新产品上线", 800L);
        assertThat(result.getLaunchDate()).isEqualTo(TARGET);
        verify(auditLogService, times(1)).append(any());
    }

    @Test
    @DisplayName("AC#3 CONFIRMED 状态 + launchDate=null ⇒ 成功")
    void confirmedStatus_succeeds() {
        Project p = draft(null);
        p.setStatus("CONFIRMED");
        when(projectMapper.selectById(900L)).thenReturn(p);
        Project result = service.initialRecord(900L, TARGET, "正式确认", 800L);
        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        assertThat(result.getLaunchDate()).isEqualTo(TARGET);
    }

    @Test
    @DisplayName("AC#4 ARCHIVED 状态 ⇒ 拒")
    void archivedStatus_rejected() {
        Project p = draft(null);
        p.setStatus("ARCHIVED");
        when(projectMapper.selectById(900L)).thenReturn(p);
        assertThatThrownBy(() -> service.initialRecord(900L, TARGET, "已归档", 800L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("DRAFT");
    }

    @Test
    @DisplayName("AC#5 launchDate 已存在 ⇒ 拒（走双签）")
    void existingLaunchDate_rejected() {
        Project p = draft(new Date(1_800_000_000_000L));
        when(projectMapper.selectById(900L)).thenReturn(p);
        assertThatThrownBy(() -> service.initialRecord(900L, TARGET, "改期", 800L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已存在");
    }

    @Test
    @DisplayName("AC#6 项目不存在 ⇒ 拒")
    void missingProject_rejected() {
        when(projectMapper.selectById(999L)).thenReturn(null);
        assertThatThrownBy(() -> service.initialRecord(999L, TARGET, "x", 800L))
            .isInstanceOf(ServiceException.class);
    }
}