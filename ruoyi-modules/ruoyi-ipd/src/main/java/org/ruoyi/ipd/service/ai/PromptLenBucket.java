package org.ruoyi.ipd.service.ai;

/**
 * R-NEW S-7：promptLen 离散 bucket 枚举。
 *
 * <p>覆盖范围：&lt;100 / 100-1K / 1K-10K / 10K-100K / ≥100K。
 * 边界口径：下界开区间，上界闭区间；每个桶之间整数边界独立判定，
 * 保证恰好 100 / 1000 / 10000 / 100000 的归桶可被单测覆盖。
 *
 * <p>Why：连续 promptLen 进日志构成「长度指纹」侧信道（R-NEW-SEC-9 LOW）；
 * 改离散桶后，仅桶标签 + 切换信号进日志，消除长度指纹同时保留「分布/告警/配额」
 * 决策能力。
 */
public enum PromptLenBucket {
    LT_100     ("lt_100",      0, 99),
    B_100_1K   ("100_to_1k",   100,   999),
    B_1K_10K   ("1k_to_10k",   1_000, 9_999),
    B_10K_100K ("10k_to_100k", 10_000, 99_999),
    GE_100K    ("ge_100k",     100_000, Integer.MAX_VALUE);

    private final String label;
    private final int min;
    private final int max;

    PromptLenBucket(String label, int min, int max) {
        this.label = label;
        this.min = min;
        this.max = max;
    }

    public String label() { return label; }
    public int min() { return min; }
    public int max() { return max; }

    /**
     * promptLen → bucket。
     *
     * <p>负数 / 0 视为 LT_100（防御性兜底）；{@code Integer.MAX_VALUE} 由 GE_100K 桶兜底，
     * 永不溢出。
     */
    public static PromptLenBucket of(int promptLen) {
        if (promptLen < 100)      return LT_100;
        if (promptLen < 1_000)    return B_100_1K;
        if (promptLen < 10_000)   return B_1K_10K;
        if (promptLen < 100_000)  return B_10K_100K;
        return GE_100K;
    }
}
