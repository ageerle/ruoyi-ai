package org.ruoyi.ipd.service.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R-NEW S-7：PromptLenBucket 离散归桶单测。
 *
 * <p>边界口径：5 个桶的 6 条分界线（0/99/100/999/1000/9999/10000/99999/100000）必须一一归桶正确。
 */
@Tag("dev")
@DisplayName("R-NEW S-7 PromptLenBucket 边界归桶")
class PromptLenBucketTest {

    @Test
    @DisplayName("B-1 0 → LT_100（下界）")
    void zero_lt100() {
        assertThat(PromptLenBucket.of(0)).isEqualTo(PromptLenBucket.LT_100);
    }

    @Test
    @DisplayName("B-2 99 → LT_100（< 100 上界）")
    void ninetyNine_lt100() {
        assertThat(PromptLenBucket.of(99)).isEqualTo(PromptLenBucket.LT_100);
    }

    @Test
    @DisplayName("B-3 100 → B_100_1K（恰好 100）")
    void hundred_b100to1k() {
        assertThat(PromptLenBucket.of(100)).isEqualTo(PromptLenBucket.B_100_1K);
    }

    @Test
    @DisplayName("B-4 999 → B_100_1K（10x100-1 临界）")
    void nineNinetyNine_b100to1k() {
        assertThat(PromptLenBucket.of(999)).isEqualTo(PromptLenBucket.B_100_1K);
    }

    @Test
    @DisplayName("B-5 1_000 → B_1K_10K（恰好 1K）")
    void thousand_b1kto10k() {
        assertThat(PromptLenBucket.of(1_000)).isEqualTo(PromptLenBucket.B_1K_10K);
    }

    @Test
    @DisplayName("B-6 9_999 → B_1K_10K（< 10K 上界）")
    void nineThousandNineNinetyNine_b1kto10k() {
        assertThat(PromptLenBucket.of(9_999)).isEqualTo(PromptLenBucket.B_1K_10K);
    }

    @Test
    @DisplayName("B-7 10_000 → B_10K_100K（恰好 10K）")
    void tenThousand_b10kto100k() {
        assertThat(PromptLenBucket.of(10_000)).isEqualTo(PromptLenBucket.B_10K_100K);
    }

    @Test
    @DisplayName("B-8 99_999 → B_10K_100K（< 100K 上界）")
    void ninetyNineThousandNineNinetyNine_b10kto100k() {
        assertThat(PromptLenBucket.of(99_999)).isEqualTo(PromptLenBucket.B_10K_100K);
    }

    @Test
    @DisplayName("B-9 100_000 → GE_100K（恰好 100K）")
    void hundredThousand_ge100k() {
        assertThat(PromptLenBucket.of(100_000)).isEqualTo(PromptLenBucket.GE_100K);
    }

    @Test
    @DisplayName("B-10 Integer.MAX_VALUE → GE_100K（不溢出）")
    void maxValue_ge100k() {
        assertThat(PromptLenBucket.of(Integer.MAX_VALUE)).isEqualTo(PromptLenBucket.GE_100K);
    }

    @Test
    @DisplayName("B-11 负数 → LT_100（防御性兜底）")
    void negative_lt100() {
        assertThat(PromptLenBucket.of(-1)).isEqualTo(PromptLenBucket.LT_100);
    }
}
