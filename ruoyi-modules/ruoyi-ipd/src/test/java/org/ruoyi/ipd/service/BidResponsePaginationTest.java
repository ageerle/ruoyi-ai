package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PERF-P0-4 + PERF-P1-2 验收测试：BidResponseService.listByRdPmPaged +
 * BidInvitationService.listResponsesPaged 物理分页重构。
 *
 * <p>核心契约：
 * <ul>
 *   <li>{@code pageSize} 上限 200（硬约束，防 DoS 滥用）</li>
 *   <li>{@code pageSize=null} → 默认 20</li>
 *   <li>IDOR 三分支放行同 listByRdPm：本人 / SUPER_ADMIN / 关联项目在职 ProjectMember</li>
 *   <li>分页参数透传 mapper.selectPage 的 Page&lt;BidResponse&gt;</li>
 *   <li>listResponsesPaged 隐私过滤（ONE_TO_ONE/PUBLIC/发起人）+ 脱敏口径不变</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class BidResponsePaginationTest {

    @Mock private BidResponseMapper bidResponseMapper;
    @Mock private BidInvitationMapper bidInvitationMapper;
    @Mock private ProjectMemberMapper projectMemberMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    private BidResponseService bidResponseService;
    private BidInvitationService bidInvitationService;

    private static final Long RD_PM_A = 200L;
    private static final Long MARKET_PM = 300L;
    private static final Long OUTSIDER = 999L;
    private static final Long CREATOR = 333L;

    private static final IpdActor SELF_ACTOR = new IpdActor(RD_PM_A, "研发PM甲", "RD_PM", 1L);
    private static final IpdActor MARKET_PM_ACTOR = new IpdActor(MARKET_PM, "市场PM", "MARKET_PM", 1L);
    private static final IpdActor ADMIN_ACTOR = new IpdActor(1L, "超管", "SUPER_ADMIN", null);
    private static final IpdActor OUTSIDER_ACTOR = new IpdActor(OUTSIDER, "路人", "RD_PM", 2L);

    private static String summary(String tail) {
        return "技术方案摘要：架构选型与里程碑拆解，含风险对策与资源投入说明，覆盖验收标准。" + tail;
    }

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-perf04-test");
        TableInfoHelper.initTableInfo(assistant, BidResponse.class);
        TableInfoHelper.initTableInfo(assistant, BidInvitation.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        bidResponseService = new BidResponseService(
            bidResponseMapper, bidInvitationMapper, projectMemberMapper, auditLogService);
        bidInvitationService = new BidInvitationService(
            bidInvitationMapper, bidResponseMapper, auditLogService, notificationService);
    }

    private BidResponse row(Long id, Long invitationId, Long rdPmId) {
        return BidResponse.builder().id(id).invitationId(invitationId).rdPmId(rdPmId)
            .status("PENDING").responseNote(summary("A方案")).build();
    }

    private BidInvitation invitation(Long id, Long projectId, String mode, Long createBy) {
        BidInvitation inv = BidInvitation.builder().id(id).projectId(projectId).mode(mode)
            .title("公开招标").status("OPEN")
            .expireAt(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)).build();
        inv.setCreateBy(createBy);
        return inv;
    }

    /** Mock 返回 Page<>（Mockito 匹配 <P extends IPage<T>> 时按具体类型推断）。 */
    private Page<BidResponse> pagedPage(long current, long size, int records) {
        Page<BidResponse> p = new Page<>(current, size);
        List<BidResponse> list = IntStream.range(0, records)
            .mapToObj(i -> row(1000L + i, 1001L, RD_PM_A)).toList();
        p.setRecords(list);
        p.setTotal((long) records);
        return p;
    }

    // ==================== PERF-P0-4 listByRdPmPaged ====================

    @Test
    @DisplayName("[PERF-P0-4-1] 本人查询 → 默认 pageSize=20, pageNo=1, IPage 物理分页")
    void listByRdPmPaged_self_defaultPage() {
        Page<BidResponse> expected = pagedPage(1, 20, 3);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));
        when(bidResponseMapper.selectPage(any(Page.class), any())).thenReturn(expected);

        IPage<BidResponse> result = bidResponseService.listByRdPmPaged(SELF_ACTOR, RD_PM_A, null, null);

        assertThat(result).isSameAs(expected);
        assertThat(result.getSize()).isEqualTo(20);
        ArgumentCaptor<Page<BidResponse>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(bidResponseMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("[PERF-P0-4-2] pageSize 上限 200：pageSize=500 → 实际 200")
    void listByRdPmPaged_pageSizeClampedTo200() {
        Page<BidResponse> expected = pagedPage(1, 200, 5);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));
        when(bidResponseMapper.selectPage(any(Page.class), any())).thenReturn(expected);

        bidResponseService.listByRdPmPaged(SELF_ACTOR, RD_PM_A, 1, 500);

        ArgumentCaptor<Page<BidResponse>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(bidResponseMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(BidResponseService.MAX_PAGE_SIZE);
        assertThat(BidResponseService.MAX_PAGE_SIZE).isEqualTo(200);
    }

    @Test
    @DisplayName("[PERF-P0-4-3] 第二页查询 pageNo=2, pageSize=10 → 物理分页透传")
    void listByRdPmPaged_secondPage() {
        Page<BidResponse> expected = pagedPage(2, 10, 10);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));
        when(bidResponseMapper.selectPage(any(Page.class), any())).thenReturn(expected);

        IPage<BidResponse> result = bidResponseService.listByRdPmPaged(SELF_ACTOR, RD_PM_A, 2, 10);

        assertThat(result.getCurrent()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getRecords()).hasSize(10);
    }

    @Test
    @DisplayName("[PERF-P0-4-4] 空集合 → 空 IPage，零 mapper.selectPage 触发（fail-closed 防御）")
    void listByRdPmPaged_emptyRows_thirdParty_failClosed() {
        when(bidResponseMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> bidResponseService.listByRdPmPaged(OUTSIDER_ACTOR, RD_PM_A, 1, 20))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
        verify(bidResponseMapper, never()).selectPage(any(Page.class), any());
    }

    @Test
    @DisplayName("[PERF-P0-4-5] SUPER_ADMIN 分页查询 → 豁免（不变）")
    void listByRdPmPaged_superAdmin_exempted() {
        Page<BidResponse> expected = pagedPage(1, 20, 3);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));
        when(bidResponseMapper.selectPage(any(Page.class), any())).thenReturn(expected);

        IPage<BidResponse> result = bidResponseService.listByRdPmPaged(ADMIN_ACTOR, RD_PM_A, 1, 20);

        assertThat(result).isSameAs(expected);
        verify(projectMemberMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("[PERF-P0-4-6] 关联项目在职成员分页查询 → 豁免（探测 + 分页）")
    void listByRdPmPaged_relatedProjectMember_exempted() {
        Page<BidResponse> expected = pagedPage(1, 20, 3);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));
        when(bidInvitationMapper.selectBatchIds(any())).thenReturn(List.of(invitation(1001L, 100L, "PUBLIC", null)));
        when(projectMemberMapper.selectCount(any())).thenReturn(1L);
        when(bidResponseMapper.selectPage(any(Page.class), any())).thenReturn(expected);

        IPage<BidResponse> result = bidResponseService.listByRdPmPaged(MARKET_PM_ACTOR, RD_PM_A, 1, 20);

        assertThat(result).isSameAs(expected);
        verify(projectMemberMapper).selectCount(any());
    }

    // ==================== PERF-P1-2 listResponsesPaged ====================

    @Test
    @DisplayName("[PERF-P1-2-1] 发起人 PUBLIC 默认分页 20 → 全量 + 不脱敏")
    void listResponsesPaged_creatorPublicDefaultPage() {
        BidInvitation inv = invitation(1001L, 100L, "PUBLIC", CREATOR);
        Page<BidResponse> expected = pagedPage(1, 20, 3);
        when(bidInvitationMapper.selectById(1001L)).thenReturn(inv);
        when(bidResponseMapper.selectPage(any(Page.class), any())).thenReturn(expected);

        IPage<BidResponse> result = bidInvitationService.listResponsesPaged(1001L, CREATOR, null, null);

        assertThat(result).isSameAs(expected);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getRecords().get(0).getResponseNote()).contains("A方案");
    }

    @Test
    @DisplayName("[PERF-P1-2-2] 非发起人 PUBLIC 默认分页 20 → 脱敏前 80 字符")
    void listResponsesPaged_nonCreatorPublic_masksSummary() {
        BidInvitation inv = invitation(1001L, 100L, "PUBLIC", CREATOR);
        Page<BidResponse> p = new Page<>(1, 20);
        p.setRecords(List.of(BidResponse.builder().id(3001L).invitationId(1001L)
            .rdPmId(555L).status("PENDING")
            .responseNote(summary("包含80字之后的机密信息：BCDEFG…")).build()));
        p.setTotal(1L);
        when(bidInvitationMapper.selectById(1001L)).thenReturn(inv);
        when(bidResponseMapper.selectPage(any(Page.class), any())).thenReturn(p);

        IPage<BidResponse> result = bidInvitationService.listResponsesPaged(1001L, MARKET_PM, 1, 20);

        assertThat(result.getRecords().get(0).getResponseNote())
            .hasSizeLessThanOrEqualTo(81)
            .endsWith("…");
    }

    @Test
    @DisplayName("[PERF-P1-2-3] ONE_TO_ONE 非发起人 → wrapper 必含 personId 等值（防信息泄露）")
    void listResponsesPaged_oneToOne_filtersByCurrentPerson() {
        BidInvitation inv = invitation(1001L, 100L, "ONE_TO_ONE", CREATOR);
        Page<BidResponse> expected = pagedPage(1, 20, 1);
        when(bidInvitationMapper.selectById(1001L)).thenReturn(inv);
        when(bidResponseMapper.selectPage(any(Page.class), any())).thenReturn(expected);

        bidInvitationService.listResponsesPaged(1001L, MARKET_PM, 1, 20);

        ArgumentCaptor<Page<BidResponse>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<LambdaQueryWrapper<BidResponse>> wrapperCaptor =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(bidResponseMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
            .contains("invitation_id")
            .contains("rd_pm_id");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values()).contains(MARKET_PM);
    }

    @Test
    @DisplayName("[PERF-P1-2-4] pageSize 上限 200：pageSize=300 → 实际 200")
    void listResponsesPaged_pageSizeClampedTo200() {
        BidInvitation inv = invitation(1001L, 100L, "PUBLIC", CREATOR);
        when(bidInvitationMapper.selectById(1001L)).thenReturn(inv);
        when(bidResponseMapper.selectPage(any(Page.class), any())).thenReturn(pagedPage(1, 200, 0));

        bidInvitationService.listResponsesPaged(1001L, CREATOR, 1, 300);

        ArgumentCaptor<Page<BidResponse>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(bidResponseMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(200);
    }

    @Test
    @DisplayName("[PERF-P1-2-5] 不存在招标单 → NOT_FOUND（不变）")
    void listResponsesPaged_invitationNotFound() {
        when(bidInvitationMapper.selectById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> bidInvitationService.listResponsesPaged(9999L, CREATOR, 1, 20))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("招标单不存在")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
        verify(bidResponseMapper, never()).selectPage(any(Page.class), any());
    }

    @Test
    @DisplayName("[PERF-P1-2-6] currentPersonId=null → PARAM_INVALID（MEDIUM-info-disclosure 修复不变）")
    void listResponsesPaged_nullActor_paramInvalid() {
        BidInvitation inv = invitation(1001L, 100L, "PUBLIC", CREATOR);
        when(bidInvitationMapper.selectById(1001L)).thenReturn(inv);

        assertThatThrownBy(() -> bidInvitationService.listResponsesPaged(1001L, null, 1, 20))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录或会话失效")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verify(bidResponseMapper, never()).selectPage(any(Page.class), any());
    }

    // ==================== 兼容性 ====================

    @Test
    @DisplayName("[COMPAT] listByRdPm（无分页）保持原签名（W5-E-2.4 IDOR 测试不回归）")
    void listByRdPm_legacyStillWorks() {
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));

        List<BidResponse> result = bidResponseService.listByRdPm(SELF_ACTOR, RD_PM_A);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("[COMPAT] listResponses（无分页）保持原签名（BidInvitationServiceTest 不回归）")
    void listResponses_legacyStillWorks() {
        BidInvitation inv = invitation(1001L, 100L, "PUBLIC", CREATOR);
        when(bidInvitationMapper.selectById(1001L)).thenReturn(inv);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));

        List<BidResponse> result = bidInvitationService.listResponses(1001L, CREATOR);

        assertThat(result).hasSize(1);
    }

    // ==================== 工具校验 ====================

    @Test
    @DisplayName("[UTILITY] MAX_PAGE_SIZE 硬上限 = 200（防 DoS 契约锁）")
    void maxPageSizeContract() {
        assertThat(BidResponseService.MAX_PAGE_SIZE).isEqualTo(200);
    }

    @Test
    @DisplayName("[UTILITY] pageNo<1 → 归一化为 1；pageSize<1 → 归一化为 1")
    void pageParamsNormalized() {
        Page<BidResponse> expected = pagedPage(1, 1, 0);
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(row(2001L, 1001L, RD_PM_A)));
        when(bidResponseMapper.selectPage(any(Page.class), any())).thenReturn(expected);

        bidResponseService.listByRdPmPaged(SELF_ACTOR, RD_PM_A, 0, 0);

        ArgumentCaptor<Page<BidResponse>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(bidResponseMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(1);
    }
}