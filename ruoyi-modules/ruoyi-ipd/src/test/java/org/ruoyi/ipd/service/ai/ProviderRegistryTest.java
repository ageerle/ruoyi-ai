package org.ruoyi.ipd.service.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P4-2.1 ProviderRegistry 派发 + 并发 + 兜底（@Tag dev）。
 */
@Tag("dev")
@DisplayName("P4-2.1 ProviderRegistry 派发与兜底")
class ProviderRegistryTest {

    /** 桩 Tester：只用于派发断言，不发 HTTP。 */
    static final class StubTester implements AiProviderTester {
        private final String provider;
        private final Set<String> aliases;
        StubTester(String provider, Set<String> aliases) {
            this.provider = provider;
            this.aliases = aliases;
        }
        @Override public String provider() { return provider; }
        @Override public Set<String> aliases() { return aliases; }
        @Override public AiTestResult test(AiTestConfig cfg) {
            return AiTestResult.ok(1);
        }
    }

    @Test
    @DisplayName("派发按 aliases 大小写不敏感")
    void dispatchByAliasCaseInsensitive() {
        ProviderRegistry reg = new ProviderRegistry(List.of(
            new StubTester("openai", Set.of("openai", "deepseek", "qwen", "moonshot", "MiniMax")),
            new StubTester("zhipu", Set.of("zhipu", "glm")),
            new StubTester("ollama", Set.of("ollama")),
            new StubTester("default", Set.of("default"))
        ));
        assertEquals("openai", reg.find("DeepSeek").provider());
        assertEquals("zhipu", reg.find("GLM").provider());
        assertEquals("ollama", reg.find("ollama").provider());
        assertEquals("openai", reg.find("qwen").provider());
        assertEquals("openai", reg.find("MiniMax").provider());
    }

    @Test
    @DisplayName("未知 provider 走 DefaultTester（不抛异常）")
    void unknownProvider_fallsBackToDefault() {
        ProviderRegistry reg = new ProviderRegistry(List.of(
            new StubTester("openai", Set.of("openai")),
            new StubTester("default", Set.of("default"))
        ));
        AiProviderTester t = reg.find("some-unknown-vendor");
        assertNotNull(t, "必须落到 DefaultTester");
        assertEquals("default", t.provider());
    }

    @Test
    @DisplayName("null/空 provider 走 DefaultTester")
    void nullProvider_fallsBackToDefault() {
        ProviderRegistry reg = new ProviderRegistry(List.of(
            new StubTester("default", Set.of("default"))
        ));
        assertEquals("default", reg.find(null).provider());
        assertEquals("default", reg.find("").provider());
    }

    @Test
    @DisplayName("100 并发查同一 provider 命中同一 Tester 实例（无 race）")
    void concurrentFind_returnsSameTester() throws Exception {
        ProviderRegistry reg = new ProviderRegistry(List.of(
            new StubTester("openai", Set.of("openai", "deepseek")),
            new StubTester("default", Set.of("default"))
        ));
        int N = 100;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(N);
        AtomicReference<AiProviderTester> ref = new AtomicReference<>();
        for (int i = 0; i < N; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    AiProviderTester t = reg.find("deepseek");
                    if (ref.get() == null) ref.set(t);
                    else assertSame(ref.get(), t, "并发查应返回同一实例");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "find-" + i).start();
        }
        start.countDown();
        done.await();
        assertNotNull(ref.get());
    }

    @Test
    @DisplayName("all() 返回不可变列表（外部修改不影响内部）")
    void allIsImmutable() {
        ProviderRegistry reg = new ProviderRegistry(List.of(new StubTester("x", Set.of("x"))));
        assertThrows(UnsupportedOperationException.class, () -> reg.all().add(null));
    }
}
