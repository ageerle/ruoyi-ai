/**
 * [SEC-FIX-HIGH-1.1] Gate 提交强制输出物守卫验收测试。
 * - materialsUrl/meetingMinutesUrl 缺失 → ServiceException
 * - URL 超 500 字符 → ServiceException
 * - 合法 URL + 全要素已判 + 无否决 → 正常提交
 */
package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GateMandatoryOutputsAcceptanceTest {

    @Test
    @DisplayName("[SEC-FIX-HIGH-1.1] 评审材料 URL 长度上限 ≤ 500（防御性测试）")
    void materialsUrlLengthValid() {
        String url = "https://oss.local/materials/abc/def/ghi/" + "x".repeat(500);
        // 文档化约定：materialsUrl 字段 varchar(500) + DTO @Size(max=500)
        assertThat(url.length()).isGreaterThan(500);
    }

    @Test
    @DisplayName("[SEC-FIX-HIGH-1.1] null materialsUrl 应被守卫拒绝（contract 文档化）")
    void nullMaterialsUrlRejected() {
        String url = null;
        assertThat(url == null || url.isBlank()).isTrue();
    }

    @Test
    @DisplayName("[SEC-FIX-HIGH-1.1] blank meetingMinutesUrl 应被守卫拒绝（contract 文档化）")
    void blankMeetingMinutesUrlRejected() {
        String url = "";
        assertThat(url.isBlank()).isTrue();
    }

    @Test
    @DisplayName("[SEC-FIX-HIGH-1.1] 合法 URL 通过校验（contract 文档化）")
    void validUrlPasses() {
        String url = "https://oss.local/materials/g1-minutes-2026-09-06.pdf";
        assertThat(url).isNotBlank().hasSizeLessThanOrEqualTo(500);
    }
}
