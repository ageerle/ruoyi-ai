package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.ruoyi.ipd.domain.NotificationChannelType;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.mapper.NotificationEventMapper;
import org.ruoyi.ipd.service.channel.EmailChannelHandler;
import org.ruoyi.ipd.service.channel.InAppChannelHandler;
import org.ruoyi.ipd.service.channel.WebSocketChannelHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知分发器（AsyncNotificationDispatcher）单测（ROOT-R2-P0-2；@Tag("dev") 必须）。
 * 7 测覆盖：
 * 1. dispatch 成功（INBOX 通道，handler 正常）
 * 2. dispatch 失败（handler 抛异常，dispatcher 抛 NotificationDispatchException）
 * 3. dispatchAsync 入队（Redisson 延迟队列收到事件 ID）
 * 4. dispatchAsync 聚合（60s 窗口内同 receiver+type 已有 PENDING → 跳过入队）
 * 5. consumeOnce 成功（事件落 PENDING → handler 投递 → status 翻 SENT）
 * 6. consumeOnce 重试 → DEAD（连续失败 3 次转 DEAD，不入重试）
 * 7. consumeOnce 幂等（已被翻 SENT 的事件再次入队 → skipped，不计 sent）
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationEventMapper mapper;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    @SuppressWarnings("rawtypes")
    private RBlockingQueue blockingQueue;
    @Mock
    @SuppressWarnings("rawtypes")
    private RDelayedQueue delayedQueue;

    private AsyncNotificationDispatcher dispatcher;
    /** 真实物理队列（绕过 mock，直接记录 Redisson 入队/出队行为） */
    private final LinkedBlockingQueue<Long> physicalQueue = new LinkedBlockingQueue<>();
    private InAppChannelHandler inAppHandler;
    private EmailChannelHandler emailHandler;
    private WebSocketChannelHandler webSocketHandler;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, NotificationEvent.class);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        physicalQueue.clear();
        inAppHandler = new InAppChannelHandler();
        emailHandler = new EmailChannelHandler();
        webSocketHandler = new WebSocketChannelHandler();
        // RedissonClient mock 链：getBlockingQueue → RDelayedQueue → 真实物理 LinkedBlockingQueue
        // 用 Answer 模式：getBlockingQueue / getDelayedQueue 都返回 mock，offer/poll 直接走物理队列
        lenient().when(redissonClient.getBlockingQueue(AsyncNotificationDispatcher.QUEUE_KEY))
            .thenReturn(blockingQueue);
        lenient().when(redissonClient.getDelayedQueue(blockingQueue)).thenReturn(delayedQueue);
        // 真实行为：delayedQueue.offer(id, 0, MS) 物理入队
        lenient().doAnswer(inv -> {
            Long eventId = inv.getArgument(0);
            physicalQueue.offer(eventId);
            return null;
        }).when(delayedQueue).offer(any(), anyLong(), any());
        // 真实行为：blockingQueue.poll(50, MS) 物理出队
        lenient().when(blockingQueue.poll(anyLong(), any())).thenAnswer(inv -> physicalQueue.poll());

        List<NotificationChannelHandler> handlers = new ArrayList<>();
        handlers.add(inAppHandler);
        handlers.add(emailHandler);
        handlers.add(webSocketHandler);
        dispatcher = new AsyncNotificationDispatcher(mapper, redissonClient, handlers);
    }

    @Test
    @DisplayName("dispatch 成功：INBOX handler 正常返回 → dispatch 不抛且返回 true")
    void dispatchSuccessInbox() {
        NotificationEvent event = baseEvent(101L, "BID_INVITED", NotificationChannelType.INBOX);
        event.setId(8001L); // INBOX handler 要求 event id 非空
        boolean ok = dispatcher.dispatch(event);
        assertThat(ok).isTrue();
    }

    @Test
    @DisplayName("dispatch 失败：handler 抛异常 → dispatcher 抛 NotificationDispatchException")
    void dispatchFailureWraps() {
        NotificationEvent event = baseEvent(102L, "BID_INVITED", NotificationChannelType.EMAIL);
        // EmailChannelHandler.deliver 抛 UnsupportedOperationException（stub 行为）
        assertThatThrownBy(() -> dispatcher.dispatch(event))
            .isInstanceOf(NotificationDispatcher.NotificationDispatchException.class)
            .hasMessageContaining("EMAIL");
    }

    @Test
    @DisplayName("dispatchAsync 入队：同 receiver+type 无 PENDING 既有 → 入 Redisson 队列")
    void dispatchAsyncEnqueuesWhenNoPending() {
        NotificationEvent event = baseEvent(201L, "GATE_REJECTED", NotificationChannelType.WEBSOCKET);
        event.setId(9001L);
        when(mapper.selectCount(any())).thenReturn(0L);
        boolean ok = dispatcher.dispatchAsync(event);
        assertThat(ok).isTrue();
        assertThat(physicalQueue).contains(9001L);
        verify(delayedQueue, times(1)).offer(eq(9001L), anyLong(), any());
    }

    @Test
    @DisplayName("dispatchAsync 聚合：60s 窗口内同 receiver+type 已有 PENDING → 跳过入队")
    void dispatchAsyncAggregatesWithinWindow() {
        NotificationEvent event = baseEvent(202L, "GATE_REJECTED", NotificationChannelType.WEBSOCKET);
        event.setId(9002L);
        // mapper.selectCount 返回 >0 表示聚合命中
        when(mapper.selectCount(any())).thenReturn(1L);
        boolean ok = dispatcher.dispatchAsync(event);
        assertThat(ok).isFalse();
        assertThat(physicalQueue).isEmpty();
        verify(delayedQueue, never()).offer(any(), anyLong(), any());
    }

    @Test
    @DisplayName("consumeOnce 成功：PENDING → handler 投递 → 条件 UPDATE 翻 SENT，sent=1")
    void consumeOnceSent() throws Exception {
        NotificationEvent event = baseEvent(301L, "BID_INVITED", NotificationChannelType.INBOX);
        event.setId(9003L);
        physicalQueue.offer(9003L);
        when(mapper.selectById(9003L)).thenReturn(event);
        when(mapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Map<String, Integer> result = dispatcher.consumeOnce(10);
        assertThat(result).containsEntry("sent", 1);
        assertThat(result).containsEntry("failed", 0);
        assertThat(result).containsEntry("dead", 0);
        verify(mapper, atLeastOnce()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("consumeOnce 重试到 DEAD：handler 连续失败 3 次后转 DEAD（不进重试队列）")
    void consumeOnceDeadAfter3Failures() throws Exception {
        NotificationEvent event = baseEvent(401L, "DEL_REJECTED", NotificationChannelType.EMAIL);
        event.setId(9004L);
        event.setRetryCount(2); // 已经失败 2 次，下次失败即转 DEAD
        physicalQueue.offer(9004L);
        when(mapper.selectById(9004L)).thenReturn(event);
        when(mapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Map<String, Integer> result = dispatcher.consumeOnce(10);
        assertThat(result).containsEntry("dead", 1);
        assertThat(result).containsEntry("failed", 0);
        assertThat(result).containsEntry("sent", 0);
        // 校验：重试转 DEAD 至少调一次 update（DEAD 终态写库）
        verify(mapper, atLeastOnce()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("consumeOnce 幂等：事件已被翻 SENT → 二次入队被 skipped，不重复 sent")
    void consumeOnceIdempotentSkipsAlreadySent() throws Exception {
        NotificationEvent event = baseEvent(501L, "GATE_SIGN_SOON", NotificationChannelType.INBOX);
        event.setId(9005L);
        event.setDeliveryStatus("SENT"); // 已被并发消费翻 SENT
        physicalQueue.offer(9005L);
        when(mapper.selectById(9005L)).thenReturn(event);

        Map<String, Integer> result = dispatcher.consumeOnce(10);
        assertThat(result).containsEntry("sent", 0);
        assertThat(result).containsEntry("skipped", 1);
        // 不应再调 handler.deliver
        // 不应再写 SENT update
        verify(mapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    private NotificationEvent baseEvent(Long receiverId, String type, NotificationChannelType channel) {
        AtomicReference<NotificationEvent> ref = new AtomicReference<>();
        NotificationEvent e = NotificationEvent.builder()
            .receiverId(receiverId)
            .eventType(type)
            .kind("FYI")
            .sourceType("bid_invitations")
            .sourceId(1L)
            .dedupKey("bid_invitations:" + type + ":1:" + receiverId)
            .title("test")
            .content("content")
            .channel("MOCK")
            .targetChannel(channel.getCode())
            .locale("zh-CN")
            .deliveryStatus("PENDING")
            .retryCount(0)
            .readFlag("0")
            .build();
        e.setCreateTime(new Date());
        e.setUpdateTime(new Date());
        ref.set(e);
        return e;
    }
}
