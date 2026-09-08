package org.ruoyi.ipd.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * R-NEW A-2：IpdIdorGuard.assertSameGroupIpd(IpdActor, Long) 守卫 6 专项单测。
 *
 * <p>覆盖 9 个边界：null actor / null id / SUPER_ADMIN 豁免 / MARKET_PM 同组通过 / 跨组拒 /
 * GROUP_LEADER 同组通过 / actor.groupId null 拒 / objectGroupId null 拒 / 两侧 null 拒。
 *
 * <p>核心断言：错误码恒等 {@link ApiV1ErrorCode#FORBIDDEN}；文案精确等 {@code "无权操作"}
 * （W4-Security 决策 3 锁定 FORBIDDEN 文案，不区分资源不存在与无权限——不泄漏存在性）。
 *
 * <p>纯 JVM 单测（无 Spring 上下文）。{@code LoginHelper.getTenantId()} 在租户禁用或未登录场景下
 * 返回 null，故跨租户分支（{@link IpdIdorGuard#requireTenantMatch}）不影响守卫 6 判定。
 */
@Tag("dev")
@DisplayName("R-NEW A-2 IpdIdorGuard.assertSameGroupIpd 守卫 6 边界")
class IpdIdorGuardSameGroupTest {

    @Test
    @DisplayName("SG-1 null actor → UNAUTHORIZED「未登录」")
    void nullActor_unauthorized() {
        assertThatThrownBy(() -> IpdIdorGuard.assertSameGroupIpd(null, 100L))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessage("未登录")
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("SG-2 actor.id() == null → UNAUTHORIZED「未登录」")
    void actorIdNull_unauthorized() {
        IpdActor actor = new IpdActor(null, "phantom", "MARKET_PM", 5L);
        assertThatThrownBy(() -> IpdIdorGuard.assertSameGroupIpd(actor, 100L))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessage("未登录")
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("SG-3 SUPER_ADMIN + actor.groupId=null + objectGroupId 任意 → 通过（豁免）")
    void superAdmin_groupIdNull_passes() {
        IpdActor admin = new IpdActor(1L, "root", "SUPER_ADMIN", null);
        assertThatCode(() -> IpdIdorGuard.assertSameGroupIpd(admin, 100L))
            .doesNotThrowAnyException();
        assertThatCode(() -> IpdIdorGuard.assertSameGroupIpd(admin, 999L))
            .doesNotThrowAnyException();
        assertThatCode(() -> IpdIdorGuard.assertSameGroupIpd(admin, null))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SG-4 MARKET_PM 同组（actor.groupId=5L == objectGroupId=5L）→ 通过")
    void marketPm_sameGroup_passes() {
        IpdActor actor = new IpdActor(11L, "market", "MARKET_PM", 5L);
        assertThatCode(() -> IpdIdorGuard.assertSameGroupIpd(actor, 5L))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SG-5 MARKET_PM 跨组（actor.groupId=5L ≠ objectGroupId=6L）→ FORBIDDEN「无权操作」")
    void marketPm_crossGroup_forbidden() {
        IpdActor actor = new IpdActor(11L, "market", "MARKET_PM", 5L);
        assertThatThrownBy(() -> IpdIdorGuard.assertSameGroupIpd(actor, 6L))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessage("无权操作")
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("SG-6 GROUP_LEADER 同组 → 通过")
    void groupLeader_sameGroup_passes() {
        IpdActor actor = new IpdActor(21L, "leader", "GROUP_LEADER", 8L);
        assertThatCode(() -> IpdIdorGuard.assertSameGroupIpd(actor, 8L))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SG-7 GROUP_LEADER actor.groupId=null → FORBIDDEN「无权操作」（不豁免，groupId 必填）")
    void groupLeader_groupIdNull_forbidden() {
        IpdActor actor = new IpdActor(21L, "leader", "GROUP_LEADER", null);
        assertThatThrownBy(() -> IpdIdorGuard.assertSameGroupIpd(actor, 8L))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessage("无权操作")
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("SG-8 actor.groupId 存在但 objectGroupId=null → FORBIDDEN「无权操作」")
    void objectGroupIdNull_forbidden() {
        IpdActor actor = new IpdActor(11L, "market", "MARKET_PM", 5L);
        assertThatThrownBy(() -> IpdIdorGuard.assertSameGroupIpd(actor, null))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessage("无权操作")
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("SG-9 两侧均 null → FORBIDDEN「无权操作」（null 永远拒绝，无「都 null 即放行」语义）")
    void bothNull_forbidden() {
        IpdActor actor = new IpdActor(11L, "market", "MARKET_PM", null);
        assertThatThrownBy(() -> IpdIdorGuard.assertSameGroupIpd(actor, null))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessage("无权操作")
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }
}