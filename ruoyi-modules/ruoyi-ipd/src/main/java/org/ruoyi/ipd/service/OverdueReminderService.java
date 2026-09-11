package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * P1-4.4 深管逾期与轻管免打扰策略。
 *
 * <p>AC-IPD-12：深管动作逾期（due_date 已过且未终态）⇒
 * <ol>
 *   <li>自动标记：状态迁移到 DELAYED（复用 P1-4.3 transit 白名单 + 审计 + 乐观锁，唯一入口不旁路）；</li>
 *   <li>每日提醒：主责人（ProjectMember.role = ownerRole 且未退出）收到 ACTION 类通知，
 *       dedupKey 按日去重——同日重扫不重发（重复扫描不多通知），次日再提醒。</li>
 * </ol>
 * AC-IPD-13：轻管动作逾期 ⇒ 不提醒、不标记（无延期枚举，避免过度打扰研发 PM）。
 *
 * <p>开关配置（G-05 零硬编码）：action.overdueReminder.enabled（SystemConfig，缺省 true）。
 * 扫描入口 scanForDate(now) 供调度底座（OPS-04）注入时钟调用；重入安全。
 */
@Service
@RequiredArgsConstructor
public class OverdueReminderService {

    public static final String CONFIG_ENABLED = "action.overdueReminder.enabled";
    public static final String SOURCE_TYPE = "stage_actions";
    /** 逾期未终态：标记候选（NOT_STARTED/IN_PROGRESS）+ 提醒候选（含已 DELAYED，每日续提醒）。 */
    private static final List<String> REMIND_STATUSES = List.of("NOT_STARTED", "IN_PROGRESS", "DELAYED");
    private static final List<String> MARK_STATUSES = List.of("NOT_STARTED", "IN_PROGRESS");

    private final StageActionMapper stageActionMapper;
    private final StageActionService stageActionService;
    private final ProjectMemberMapper projectMemberMapper;
    private final NotificationService notificationService;
    private final SystemConfigService systemConfigService;

    /** 扫描结果计数（验收与运营观测用）。 */
    public record ScanResult(int scanned, int deepMarked, int deepReminded, int lightSkipped, int markSkipped) {
    }

    /**
     * 以指定时点扫描全部逾期动作并执行 标记+提醒。
     *
     * @param now 业务时钟（OPS-04 可注入；测试注入固定日期）
     * @return 计数结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ScanResult scanForDate(Date now) {
        if (!systemConfigService.getBoolValue(CONFIG_ENABLED, true)) {
            return new ScanResult(0, 0, 0, 0, 0);
        }
        List<StageAction> overdue = stageActionMapper.selectList(new LambdaQueryWrapper<StageAction>()
            .isNotNull(StageAction::getDueDate)
            .lt(StageAction::getDueDate, now)
            .in(StageAction::getStatus, REMIND_STATUSES));

        int marked = 0;
        int reminded = 0;
        int lightSkipped = 0;
        int markSkipped = 0;
        for (StageAction a : overdue) {
            if (!"DEEP".equals(a.getDepth())) {
                lightSkipped++; // AC-IPD-13：轻管不提醒、不标记
                continue;
            }
            if (MARK_STATUSES.contains(a.getStatus())) {
                try {
                    stageActionService.transit(a.getId(), "DELAYED", "深管逾期自动标记（P1-4.4 逾期扫描）", "SYSTEM");
                    marked++;
                } catch (ServiceException e) {
                    markSkipped++; // 项目不可写/并发乐观锁冲突等：跳过不中断整轮扫描
                }
            }
            Long receiver = resolveOwner(a.getProjectId(), a.getOwnerRole());
            if (receiver != null) {
                notificationService.publishDaily(receiver, NotificationService.Types.ACTION_OVERDUE,
                    NotificationService.KIND_ACTION, SOURCE_TYPE, a.getId(),
                    "动作逾期提醒：" + a.getActionName(),
                    "深管动作《" + a.getActionName() + "》已逾期，请尽快处理（AC-IPD-12 每日提醒）",
                    "/projects/" + a.getProjectId() + "/actions", now);
                reminded++;
            }
        }
        return new ScanResult(overdue.size(), marked, reminded, lightSkipped, markSkipped);
    }

    /** 主责人 = 项目在册成员中承担该动作 ownerRole 且未退出者（exitDate 为空）。 */
    private Long resolveOwner(Long projectId, String ownerRole) {
        if (projectId == null || ownerRole == null) {
            return null;
        }
        ProjectMember member = projectMemberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, projectId)
            .eq(ProjectMember::getRole, ownerRole)
            .isNull(ProjectMember::getExitDate)
            .last("limit 1"));
        return member == null ? null : member.getPersonId();
    }
}
