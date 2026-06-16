package org.ruoyi.common.trace.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 链路追踪详情视图对象。
 * <p>
 * 包含运行信息、节点树以及统计摘要。
 */
@Data
public class TraceDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private TraceRunVo run;
    private List<TraceNodeVo> nodes;
    private TraceStatistics statistics;

    /**
     * 链路追踪统计摘要，帮助快速了解整体执行情况。
     */
    @Data
    public static class TraceStatistics implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 总节点数 */
        private int totalNodes;

        /** 成功节点数 */
        private int successCount;

        /** 失败节点数 */
        private int failedCount;

        /** 运行中节点数 */
        private int runningCount;

        /** 最大调用深度 */
        private int maxDepth;

        /** 平均耗时 (ms) */
        private long avgDurationMs;

        /** 总链路耗时 (ms) */
        private long totalDurationMs;

        /** 慢节点 Top N */
        private List<SlowNodeInfo> topSlowNodes = new ArrayList<>();
    }

    /**
     * 慢节点简要信息。
     */
    @Data
    public static class SlowNodeInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 节点 ID */
        private String nodeId;

        /** 节点展示名称 */
        private String nodeDisplayName;

        /** 节点类型中文标签 */
        private String nodeTypeLabel;

        /** 耗时 (ms) */
        private long durationMs;

        /** 占总耗时百分比 */
        private double percentOfTotal;
    }
}
