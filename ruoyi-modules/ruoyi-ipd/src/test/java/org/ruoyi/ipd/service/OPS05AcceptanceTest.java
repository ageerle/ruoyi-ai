package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.mapper.NotificationEventMapper;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OPS-05 站内通知与可执行待办事件接口验收（AC-TEAM/GATE/DEL 通知场景底座）。
 *
 * <p>形态为 Mockito 单元验收（对齐 P033 惯例）；真库表结构探针见
 * docs/ipd-系统说明/验收/OPS-05-runner-ops05-20260905.md。
 * 单测绿不等于业务闭环：业务侧发布接线（Bid/Gate/Deletion 下游卡）与调度轮询
 * （OPS-04 scheduler 合入后）不在本卡范围。
 */
@Tag("dev")
@DisplayName("OPS05 站内通知与待办事件：outbox 发布/投递/重试/已读/隔离")
@ExtendWith(MockitoExtension.class)
class OPS05AcceptanceTest {

    @Mock
    private NotificationEventMapper mapper;

    @Mock
    private NotificationChannel channel;

    @InjectMocks
    private NotificationService service;

    /** 固定时钟：退避断言可精确到毫秒 */
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneId.of("UTC"));

    /** 渠道双抽象方法（code+send），非函数式接口 → 匿名实现 */
    private static NotificationChannel okChannel() {
        return new NotificationChannel() {
            @Override public String code() { return MockNotificationChannel.CODE; }

            @Override public void send(NotificationEvent event) { }
        };
    }

    private static NotificationChannel failingChannel() {
        return new NotificationChannel() {
            @Override public String code() { return MockNotificationChannel.CODE; }

            @Override public void send(NotificationEvent event) { throw new IllegalStateException("mock channel down"); }
        };
    }

    @BeforeAll
    static void initTableInfo() {
        // 纯 Mockito JVM 无 mapper 注册环节，lambda 列解析需显式初始化（对齐 P033）
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), ""), NotificationEvent.class);
    }

    @org.junit.jupiter.api.BeforeEach
    void stubChannelCode() {
        // publish 落 channel 列需要；lenient 防个别用例未触发的严格桩报错
        org.mockito.Mockito.lenient().when(channel.code()).thenReturn(MockNotificationChannel.CODE);
    }

    private static NotificationEvent row(long id, String status, Integer retryCount, Date nextRetryAt, Long receiverId) {
        return NotificationEvent.builder()
            .id(id).receiverId(receiverId).eventType("BID_INVITED").kind("ACTION")
            .sourceType("bid_invitations").sourceId(100L)
            .dedupKey("bid_invitations:BID_INVITED:100:" + receiverId)
            .title("t").channel("MOCK")
            .deliveryStatus(status).retryCount(retryCount).nextRetryAt(nextRetryAt)
            .readFlag("0").build();
    }

    @Test
    @DisplayName("发布：落 PENDING 行，dedup=source:event:sourceId:receiver，channel=MOCK，kind 分流")
    void publish_insertsPendingRowWithMockChannel() {
        NotificationEvent inserted = service.publish(9L, NotificationService.Types.DEL_CROSS_GROUP_CC,
            NotificationService.KIND_FYI, "deletion_requests", 7L, "跨组删除知会", "协同组组长知会", null);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(mapper).insert(captor.capture());
        NotificationEvent row = captor.getValue();
        assertThat(row.getDedupKey()).isEqualTo("deletion_requests:DEL_CROSS_GROUP_CC:7:9");
        assertThat(row.getDeliveryStatus()).isEqualTo("PENDING");
        assertThat(row.getChannel()).isEqualTo("MOCK");
        assertThat(row.getKind()).isEqualTo("FYI");
        assertThat(row.getReadFlag()).isEqualTo("0");
        assertThat(row.getRetryCount()).isZero();
        assertThat(inserted).isSameAs(row);
    }

    @Test
    @DisplayName("发布幂等：dedup 撞库返回既有行，不二次插入（同一事件只投一次）")
    void publish_duplicateDedupKey_returnsExistingOnce() {
        NotificationEvent existing = row(1L, "SENT", 0, null, 9L);
        when(mapper.insert(any(NotificationEvent.class))).thenThrow(new DuplicateKeyException("uk_notify_dedup"));
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        NotificationEvent result = service.publish(9L, NotificationService.Types.BID_INVITED,
            NotificationService.KIND_ACTION, "bid_invitations", 100L, "邀标", null, "/bid/100");

        assertThat(result).isSameAs(existing);
        verify(mapper, times(1)).insert(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("AC-TEAM-05：中标 1 人 + 落选 2 人 = 3 行，receiver 维度独立去重")
    void publish_sameEventDifferentReceivers_independentRows() {
        service.publish(11L, NotificationService.Types.BID_WON, NotificationService.KIND_ACTION,
            "bid_invitations", 100L, "中标", null, null);
        service.publish(12L, NotificationService.Types.BID_LOST, NotificationService.KIND_FYI,
            "bid_invitations", 100L, "落选", null, null);
        service.publish(13L, NotificationService.Types.BID_LOST, NotificationService.KIND_FYI,
            "bid_invitations", 100L, "落选", null, null);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(mapper, times(3)).insert(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(NotificationEvent::getDedupKey)
            .containsExactlyInAnyOrder(
                "bid_invitations:BID_WON:100:11",
                "bid_invitations:BID_LOST:100:12",
                "bid_invitations:BID_LOST:100:13");
    }

    @Test
    @DisplayName("发布校验：kind 仅 FYI|ACTION；receiver/title 必填")
    void publish_invalidArgs_rejected() {
        assertThatThrownBy(() -> service.publish(9L, "X", "NOTICE", "bid_invitations", 1L, "t", null, null))
            .isInstanceOf(IpdBusinessException.class);
        assertThatThrownBy(() -> service.publish(null, "X", NotificationService.KIND_FYI, "bid_invitations", 1L, "t", null, null))
            .isInstanceOf(IpdBusinessException.class);
        assertThatThrownBy(() -> service.publish(9L, "X", NotificationService.KIND_FYI, "bid_invitations", 1L, " ", null, null))
            .isInstanceOf(IpdBusinessException.class);
        verify(mapper, never()).insert(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("收件箱：查询恒绑 receiver_id；unreadOnly 追加未读条件（AC-TEAM-01 隔离底座）")
    void inbox_receiverScoped() {
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row(1L, "SENT", 0, null, 9L)));

        List<NotificationEvent> result = service.inbox(9L, true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<NotificationEvent>> captor =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertThat(sql).contains("receiver_id").contains("read_flag");
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("未读计数：恒按 receiver_id + 未读条件")
    void unreadCount_scoped() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        assertThat(service.unreadCount(9L)).isEqualTo(3L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<NotificationEvent>> captor =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectCount(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("receiver_id").contains("read_flag");
    }

    @Test
    @DisplayName("标记已读：本人事件置 read_flag=1 + read_at；条件更新含 receiver 守卫")
    void markRead_byOwner() {
        NotificationEvent own = row(1L, "SENT", 0, null, 9L);
        when(mapper.selectById(1L)).thenReturn(own);

        NotificationEvent result = service.markRead(1L, 9L);

        assertThat(result.getReadFlag()).isEqualTo("1");
        assertThat(result.getReadAt()).isNotNull();
        verify(mapper).update(isNull(), any());
    }

    @Test
    @DisplayName("标记已读：他人事件按 NOT_FOUND 拒绝且零更新（防探测）")
    void markRead_byNonOwner_notFound() {
        NotificationEvent others = row(1L, "SENT", 0, null, 8L);
        when(mapper.selectById(1L)).thenReturn(others);

        assertThatThrownBy(() -> service.markRead(1L, 9L))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("不存在");
        verify(mapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("标记已读幂等：已读行直接返回，不再更新")
    void markRead_alreadyRead_noUpdate() {
        NotificationEvent read = row(1L, "SENT", 0, null, 9L);
        read.setReadFlag("1");
        when(mapper.selectById(1L)).thenReturn(read);

        assertThat(service.markRead(1L, 9L).getReadFlag()).isEqualTo("1");
        verify(mapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("投递：成功行经条件 UPDATE 翻 SENT（status IN 守卫防并发双发）")
    void dispatch_success_marksSent() {
        service = new NotificationService(mapper, okChannel()).withClock(FIXED);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row(1L, "PENDING", 0, null, 9L)));
        when(mapper.update(isNull(), any())).thenReturn(1);

        Map<String, Integer> result = service.dispatchPending(50);

        assertThat(result).containsEntry("sent", 1).containsEntry("failed", 0)
            .containsEntry("dead", 0).containsEntry("skipped", 0);
        verify(mapper).update(isNull(), any());
    }

    @Test
    @DisplayName("投递并发守卫：send 成功但条件 UPDATE 影响 0 行 → skipped 不计 sent")
    void dispatch_concurrentSkip() {
        service = new NotificationService(mapper, okChannel()).withClock(FIXED);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row(1L, "PENDING", 0, null, 9L)));
        when(mapper.update(isNull(), any())).thenReturn(0);

        Map<String, Integer> result = service.dispatchPending(50);

        assertThat(result).containsEntry("sent", 0).containsEntry("skipped", 1);
    }

    @Test
    @DisplayName("投递重试：retry=1 失败 → FAILED+count2+退避10min；retry=2 失败 → DEAD（死信可观察）")
    void dispatch_failure_backoffThenDead() {
        service = new NotificationService(mapper, failingChannel()).withClock(FIXED);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            row(1L, "FAILED", 1, new Date(FIXED.instant().toEpochMilli() - 1000), 9L),
            row(2L, "FAILED", 2, new Date(FIXED.instant().toEpochMilli() - 1000), 9L)));

        Map<String, Integer> result = service.dispatchPending(50);

        assertThat(result).containsEntry("failed", 1).containsEntry("dead", 1).containsEntry("sent", 0);

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<NotificationEvent>> captor =
            ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(mapper, times(2)).update(isNull(), captor.capture());
        Map<String, Object> firstParams = captor.getAllValues().get(0).getParamNameValuePairs();
        Map<String, Object> secondParams = captor.getAllValues().get(1).getParamNameValuePairs();
        Date expectedBackoff = Date.from(FIXED.instant().plusSeconds(
            NotificationService.BACKOFF_BASE_MINUTES * 60 * 2));
        assertThat(firstParams.values()).contains("FAILED", 2, expectedBackoff);
        assertThat(secondParams.values()).contains("DEAD", 3);
    }

    @Test
    @DisplayName("消费端筛选：仅扫 PENDING/FAILED 且退避期满（next_retry_at 条件在查询里）")
    void dispatch_dueFilterInQuery() {
        service = new NotificationService(mapper, okChannel()).withClock(FIXED);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.dispatchPending(50);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<NotificationEvent>> captor =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
            .contains("delivery_status").contains("next_retry_at");
    }

    @Test
    @DisplayName("一期 Mock 渠道标识：code=MOCK，send 仅留痕不抛（SENT+MOCK ≠ 站外已送达）")
    void mockChannel_markerContract() {
        MockNotificationChannel channel = new MockNotificationChannel();
        assertThat(channel.code()).isEqualTo("MOCK");
        org.assertj.core.api.Assertions.assertThatCode(() -> channel.send(row(1L, "PENDING", 0, null, 9L)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FYI 与 ACTION 分开：目录场景两类事件各自成行（AC-DEL-04 知会 vs 审批待办）")
    void kind_fyiAndActionSeparated() {
        service.publish(20L, NotificationService.Types.DEL_CROSS_GROUP_CC, NotificationService.KIND_FYI,
            "deletion_requests", 7L, "跨组删除知会", null, null);
        service.publish(21L, NotificationService.Types.GATE_SIGN_SOON, NotificationService.KIND_ACTION,
            "gate_reviews", 8L, "签署期限前 1 天", null, "/gate/8/sign");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(mapper, times(2)).insert(captor.capture());
        List<NotificationEvent> rows = captor.getAllValues();
        assertThat(rows.get(0).getKind()).isEqualTo("FYI");
        assertThat(rows.get(0).getActionUrl()).isNull();
        assertThat(rows.get(1).getKind()).isEqualTo("ACTION");
        assertThat(rows.get(1).getActionUrl()).isEqualTo("/gate/8/sign");
    }
}
