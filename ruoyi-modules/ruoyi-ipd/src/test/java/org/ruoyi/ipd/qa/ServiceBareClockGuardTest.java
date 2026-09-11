package org.ruoyi.ipd.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 时钟静态守卫（根除建议文档 §四 层 2 项 5）：service 层业务代码不得新增裸时钟调用。
 *
 * <p>存量白名单登记于 2026-09-08（47 文件 124 处实测）：AM-CLOCK-1/2 根除后剩余的
 * 「字段赋默认值」低风险形态 + static 上下文例外（BidInvitationService.ageOfInvitationMillis、
 * GuestDemandService.InMemoryHourRateLimiter——静态方法不可达实例 clock 缝，属禁一白名单）。
 *
 * <p>规则（双向强制，白名单与实测必须完全一致）：
 * <ul>
 *   <li>白名单外文件出现裸时钟 → 红。新代码的业务时钟一律走注入 Clock
 *       （范本 DefaultStateMachineGuard / DeletionRequestService，规范见
 *       docs/ipd-系统说明/治理/测试编写三禁-20260908.md 禁一）。</li>
 *   <li>白名单文件裸时钟清零后未从白名单移除 → 红（逼白名单只减不增）。</li>
 * </ul>
 */
@Tag("dev")
class ServiceBareClockGuardTest {

    /** 裸时钟形态：构造当前时刻 / 取当前时刻 / 取当前毫秒（带参 new Date(long) 是转换，不在禁区）。 */
    private static final Pattern[] BARE_CLOCK_PATTERNS = {
        Pattern.compile("new Date\\(\\)"),
        Pattern.compile("Instant\\.now\\(\\)"),
        Pattern.compile("System\\.currentTimeMillis\\(\\)"),
        Pattern.compile("LocalDateTime\\.now\\(\\)"),
        Pattern.compile("LocalDate\\.now\\(\\)"),
        Pattern.compile("OffsetDateTime\\.now\\(\\)"),
        Pattern.compile("ZonedDateTime\\.now\\(\\)"),
    };

    /**
     * 存量白名单（类名登记，2026-09-08 实测 47 文件）。只减不增：
     * 文件内裸时钟清零后同步删除条目；确需新增裸时钟（如 static 工具、协议时间戳）
     * 须登记本白名单并在 PR 描述注明理由。
     */
    private static final Set<String> LEGACY_WHITELIST = Set.of(
        "AiChatClient", "AuditLogService", "BaiduTester", "BidInvitationService",
        "BidResponseService", "BonusPoolService", "BusinessConfigService",
        "CertTemplateService", "CoefficientChangeService", "ComplianceService",
        "ContributionService", "CorrectionLogService", "DefaultStateMachineGuard",
        "DefaultTester", "DeleteAuditService", "DeletionArchiveService", "GateService",
        "GuestDemandService", "HrSyncService", "IpdAuthService", "IpdReportService",
        "KpiRecordRuleVersionService", "KpiRecordService", "KpiSharedCollectionService",
        "KpiSharedConfirmService", "LaunchDateChangeService", "LegacyImportService",
        "NegativeFeedbackService",
        "OllamaTester", "OpenAiCompatibleTester", "PersonSyncService",
        "PostLaunchReviewService", "ProductGroupService", "ProductService",
        "ProjectBootstrapService", "ProjectCertService", "ProjectMemberService",
        "ProjectScoreArchiveService", "ProjectScoreScheduleService", "ReceiptLedgerService",
        "RequirementChangeService", "RequirementStateMachine", "SopTemplateService",
        "StageActionService", "SystemConfigService", "WebSocketChannelHandler",
        "WorkbenchService", "ZhipuTester");

    private static final Path SERVICE_ROOT = Paths.get("src/main/java/org/ruoyi/ipd/service");

    @Test
    @DisplayName("1) service 层白名单外文件不得出现裸时钟（新增即红，时钟债只减不增）")
    void noBareClockOutsideWhitelist() throws IOException {
        Set<String> actual = scan().keySet();
        Set<String> violations = new TreeSet<>(actual);
        violations.removeAll(LEGACY_WHITELIST);
        assertThat(violations).as(
            "白名单外文件的裸时钟调用（新增业务时钟请走注入 Clock，"
                + "见 治理/测试编写三禁-20260908.md 禁一；确属低风险默认值/static 例外的，"
                + "登记白名单并在 PR 注明理由）").isEmpty();
    }

    @Test
    @DisplayName("2) 白名单只减不增：文件已清零的条目必须同步移除")
    void whitelistMustShrinkWhenFilesCleaned() throws IOException {
        Set<String> actual = scan().keySet();
        Set<String> stale = new TreeSet<>(LEGACY_WHITELIST);
        stale.removeAll(actual);
        assertThat(stale).as(
            "白名单中已无裸时钟的条目（文件已清零，请从 LEGACY_WHITELIST 删除，"
                + "保持白名单与实测一致）").isEmpty();
    }

    /** 扫 service 根下全部 .java（stripComments 剔注释后匹配），返回 类名 -> 裸时钟处数。 */
    private static Map<String, Integer> scan() throws IOException {
        assertThat(Files.isDirectory(SERVICE_ROOT))
            .as("service 源码根存在: " + SERVICE_ROOT.toAbsolutePath()).isTrue();
        Map<String, Integer> result = new TreeMap<>();
        try (Stream<Path> files = Files.walk(SERVICE_ROOT)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String code = GuardSourceUtils.stripComments(Files.readString(p));
                    int n = 0;
                    for (Pattern pat : BARE_CLOCK_PATTERNS) {
                        n += (int) pat.matcher(code).results().count();
                    }
                    if (n > 0) {
                        String cls = p.getFileName().toString().replace(".java", "");
                        result.merge(cls, n, Integer::sum);
                    }
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        }
        return result;
    }
}
