package org.ruoyi.service.chat.impl.provider.atlas;

import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.sse.ServerSentEvent;
import dev.langchain4j.http.client.sse.ServerSentEventContext;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class AtlasDeadlineHttpClientTest {
    static final String TEXT = "data: {\"choices\":[{\"delta\":{\"content\":\"正文\"}}]}\n\n";

    @Test void heartbeatBodyTimesOutAndClosesUpstreamWithoutRetry() throws Exception {
        exercise(false, false, Duration.ofMillis(400), Duration.ofMillis(1000));
    }

    @Test void firstTextStopsFirstDeadlineButTotalDeadlineStillClosesUpstream() throws Exception {
        exercise(true, false, Duration.ofMillis(400), Duration.ofMillis(700));
    }

    @Test void doneClosesAnOtherwiseOpenBodyAndCancelsBothDeadlines() throws Exception {
        exercise(true, true, Duration.ofMillis(400), Duration.ofMillis(700));
    }

    private void exercise(boolean text, boolean done, Duration first, Duration total) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ExecutorService workers = Executors.newCachedThreadPool(); server.setExecutor(workers);
        AtomicInteger requests = new AtomicInteger(); CountDownLatch upstreamClosed = new CountDownLatch(1);
        server.createContext("/stream", exchange -> {
            requests.incrementAndGet(); exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (var output = exchange.getResponseBody()) {
                if (text) { output.write(TEXT.getBytes(StandardCharsets.UTF_8)); output.flush(); }
                if (done) { output.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8)); output.flush(); }
                while (!Thread.currentThread().isInterrupted()) {
                    output.write(": upstream heartbeat\n\n".getBytes(StandardCharsets.UTF_8)); output.flush();
                    try { Thread.sleep(30); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); break; }
                }
            } catch (java.io.IOException closed) { upstreamClosed.countDown(); }
            finally { exchange.close(); }
        });
        server.start(); Probe probe = new Probe();
        try {
            HttpClient client = new AtlasDeadlineHttpClientBuilder(first, total).build();
            client.execute(request(server), probe);
            assertTrue(probe.terminal.await(3, TimeUnit.SECONDS), "Must terminate an opened SSE body");
            assertTrue(upstreamClosed.await(2, TimeUnit.SECONDS), "Must close the upstream, not only signal the UI");
            Thread.sleep(total.toMillis() + 100);
            assertEquals(1, requests.get(), "A deadline must not retry a paid model request");
            assertEquals(done ? 0 : 1, probe.errors.get());
            assertEquals(done ? 1 : 0, probe.closes.get());
            assertEquals(text ? 1 : 0, probe.textEvents.get());
            if (!done) assertInstanceOf(TimeoutException.class, probe.failure.get());
        } finally { server.stop(0); workers.shutdownNow(); }
    }

    @Test void cancelsRequestThatNeverReturnsHeaders() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ExecutorService workers = Executors.newCachedThreadPool(); server.setExecutor(workers);
        AtomicInteger requests = new AtomicInteger(); CountDownLatch release = new CountDownLatch(1);
        server.createContext("/stream", exchange -> {
            requests.incrementAndGet();
            try { release.await(3, TimeUnit.SECONDS); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start(); Probe probe = new Probe();
        try {
            new AtlasDeadlineHttpClientBuilder(Duration.ofMillis(300), Duration.ofMillis(800)).build().execute(request(server), probe);
            assertTrue(probe.terminal.await(2, TimeUnit.SECONDS));
            assertEquals(1, probe.errors.get()); assertEquals(0, probe.closes.get()); assertEquals(1, requests.get());
        } finally { release.countDown(); server.stop(0); workers.shutdownNow(); }
    }

    @Test void roleReasoningAndKeepalivesAreNotFirstScreenplayText() {
        assertFalse(AtlasDeadlineHttpClientBuilder.hasTextContent(": heartbeat"));
        assertFalse(AtlasDeadlineHttpClientBuilder.hasTextContent("{\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}"));
        assertFalse(AtlasDeadlineHttpClientBuilder.hasTextContent("{\"choices\":[{\"delta\":{\"reasoning_content\":\"思考\"}}]}"));
        assertTrue(AtlasDeadlineHttpClientBuilder.hasTextContent(TEXT.substring(6).trim()));
    }

    private static HttpRequest request(HttpServer server) {
        return HttpRequest.builder().method(HttpMethod.GET).url("http://127.0.0.1:" + server.getAddress().getPort() + "/stream").build();
    }

    private static final class Probe implements ServerSentEventListener {
        final AtomicInteger errors = new AtomicInteger(), closes = new AtomicInteger(), textEvents = new AtomicInteger();
        final AtomicReference<Throwable> failure = new AtomicReference<>(); final CountDownLatch terminal = new CountDownLatch(1);
        @Override public void onEvent(ServerSentEvent event, ServerSentEventContext context) {
            if (AtlasDeadlineHttpClientBuilder.hasTextContent(event.data())) textEvents.incrementAndGet();
        }
        @Override public void onError(Throwable error) { failure.set(error); errors.incrementAndGet(); terminal.countDown(); }
        @Override public void onClose() { closes.incrementAndGet(); terminal.countDown(); }
    }
}
