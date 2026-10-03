package org.ruoyi.common.sse.core;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class SseTerminalDeliveryTest {
    private SseEmitterManager manager;
    private Map<String, SseEmitter> emitters;
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("scheduler", mock(ScheduledExecutorService.class));
        new SpringUtil().postProcessBeanFactory(beans);
        manager = new SseEmitterManager();
        emitters = (Map<String, SseEmitter>) ReflectionTestUtils.getField(SseEmitterManager.class, "SESSION_EMITTERS");
        emitters.clear();
    }

    @Test void doneIsSentBeforeRemovalAndCompletion() throws Exception { assertTerminal(SseEventDto.done(), false); }
    @Test void redisTerminalErrorIsSentBeforeRemovalAndCompletion() throws Exception {
        var error = SseEventDto.error("safe failure"); error.setDone(true); assertTerminal(error, true);
    }

    private void assertTerminal(SseEventDto terminal, boolean viaRedisListener) throws Exception {
        var emitter = mock(SseEmitter.class); emitters.put("s", emitter);
        doAnswer(call -> { assertSame(emitter, emitters.get("s")); return null; }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        doAnswer(call -> { assertFalse(emitters.containsKey("s")); return null; }).when(emitter).complete();
        if (viaRedisListener) manager.sendEvent("s", terminal); else assertTrue(manager.sendLocalEvent("s", terminal));
        var ordered = inOrder(emitter); ordered.verify(emitter).send(any(SseEmitter.SseEventBuilder.class)); ordered.verify(emitter).complete();
        assertFalse(manager.sendLocalEvent("s", terminal));
    }

    @Test void normalContentAndRecoverableErrorKeepTheConnectionOpen() throws Exception {
        var emitter = mock(SseEmitter.class); emitters.put("s", emitter);
        assertTrue(manager.sendLocalEvent("s", SseEventDto.content("正文")));
        assertTrue(manager.sendLocalEvent("s", SseEventDto.error("recoverable")));
        assertSame(emitter, emitters.get("s")); verify(emitter, never()).complete();
    }
}
