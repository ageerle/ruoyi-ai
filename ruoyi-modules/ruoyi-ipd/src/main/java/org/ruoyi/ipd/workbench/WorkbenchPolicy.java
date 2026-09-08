package org.ruoyi.ipd.workbench;

import java.util.List;
import java.util.Objects;

/**
 * 工作台聚合共用口径：责任匹配 + 未完成态。
 * WorkbenchService.currentAdvance 与 StageSignAggregator 共享，避免双份漂移。
 */
public final class WorkbenchPolicy {

    /** 深管/轻管共用的未完成态（P1-4.3：DELAYED 仅深管存在，统一计入待办）。 */
    public static final List<String> OPEN_STATUSES = List.of("NOT_STARTED", "IN_PROGRESS", "DELAYED");

    private WorkbenchPolicy() {
    }

    /** 责任匹配：MARKET_PM/RD_PM 定向；组长/超管/联合（BOTH）/空值全员。owner_role 值域见 DDL：MARKET_PM|RD_PM|BOTH。 */
    public static boolean isMine(String ownerRole, String personRole) {
        if (ownerRole == null || ownerRole.isBlank() || "BOTH".equals(ownerRole) || "JOINT".equals(ownerRole)) {
            return true;
        }
        if ("SUPER_ADMIN".equals(personRole) || "GROUP_LEADER".equals(personRole)) {
            return true;
        }
        return Objects.equals(ownerRole, personRole);
    }
}
