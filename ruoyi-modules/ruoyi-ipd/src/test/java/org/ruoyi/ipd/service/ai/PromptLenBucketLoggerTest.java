package org.ruoyi.ipd.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R-NEW S-7：PromptLenBucketLogger 切换行为单测。
 *
 * <p>核心断言：桶切换时 record() 返回 true + counter 自上次切换起累计；
 * 同桶连续 record() 返回 false + counter 自增。
 */
@Tag("dev")
@DisplayName("R-NEW S-7 PromptLenBucketLogger 切换行为")
class PromptLenBucketLoggerTest {

    private PromptLenBucketLogger logger;

    @BeforeEach
    void setUp() {
        logger = new PromptLenBucketLogger();
    }

    @Test
    @DisplayName("T-1 初始 LT_100 → record(50) 同桶 → false（不切换），counter=1")
    void sameBucket_noTransition() {
        assertThat(logger.record(50)).isFalse();
        assertThat(logger.countSinceTransition()).isEqualTo(1);
        assertThat(logger.currentBucket()).isEqualTo(PromptLenBucket.LT_100);
    }

    @Test
    @DisplayName("T-2 record(50/80/99) 同桶三次 + record(200) 切换 → 第四次返回 true；counter 在切换后归零")
    void transitionResetCounter() {
        logger.record(50);
        logger.record(80);
        logger.record(99);  // 同桶
        assertThat(logger.countSinceTransition()).isEqualTo(3);
        assertThat(logger.record(200)).isTrue(); // 切换 B_100_1K
        assertThat(logger.currentBucket()).isEqualTo(PromptLenBucket.B_100_1K);
        assertThat(logger.countSinceTransition()).isEqualTo(0);   // 切完归零
    }

    @Test
    @DisplayName("T-3 多次跨桶：100→1k→10k→100k 连续切换 3 次均返回 true")
    void multiTransition_allTrue() {
        assertThat(logger.record(100)).isTrue();
        assertThat(logger.record(1_000)).isTrue();
        assertThat(logger.record(10_000)).isTrue();
        assertThat(logger.record(100_000)).isTrue();
        assertThat(logger.currentBucket()).isEqualTo(PromptLenBucket.GE_100K);
    }

    @Test
    @DisplayName("T-4 5 个边界值跨桶切换各一次均 true")
    void fiveBoundaryValues_eachTransitions() {
        logger.record(50); // LT_100（与初始同桶）
        assertThat(logger.record(100)).isTrue();     // → B_100_1K
        assertThat(logger.record(1_000)).isTrue();   // → B_1K_10K
        assertThat(logger.record(10_000)).isTrue();  // → B_10K_100K
        assertThat(logger.record(100_000)).isTrue(); // → GE_100K
    }

    @Test
    @DisplayName("T-5 桶内连续调用 100 次均 false + counter 累加到 100（先切到 B_100_1K）")
    void manySameBucket_incrementsCounter() {
        // 先把初始桶 LT_100 切到 B_100_1K，否则第一次 record(500) 会触发 LT_100 → B_100_1K 切换返回 true
        logger.record(200);
        assertThat(logger.currentBucket()).isEqualTo(PromptLenBucket.B_100_1K);
        assertThat(logger.countSinceTransition()).isEqualTo(0);
        for (int i = 0; i < 100; i++) {
            assertThat(logger.record(500)).isFalse();
        }
        assertThat(logger.countSinceTransition()).isEqualTo(100);
        assertThat(logger.currentBucket()).isEqualTo(PromptLenBucket.B_100_1K);
    }

    @Test
    @DisplayName("T-6 同桶→异桶→同桶：第二次回桶不切（prev 已是 next，CAS 短路 false）")
    void returnToSameBucket_notTransition() {
        logger.record(200);              // → B_100_1K
        logger.record(50);               // → LT_100
        assertThat(logger.countSinceTransition()).isEqualTo(0);
        assertThat(logger.record(50)).isFalse();  // 已在 LT_100
        assertThat(logger.countSinceTransition()).isEqualTo(1);
    }
}
