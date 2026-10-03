package org.ruoyi.service.chat.impl.provider.atlas;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.sse.ServerSentEvent;
import dev.langchain4j.http.client.sse.ServerSentEventContext;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Bounds an opened Atlas SSE body as well as the wait for response headers. No request retries. */
public final class AtlasDeadlineHttpClientBuilder implements HttpClientBuilder {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ScheduledExecutorService DEADLINES = Executors.newScheduledThreadPool(2, task -> {
        Thread thread = new Thread(task, "atlas-stream-deadline"); thread.setDaemon(true); return thread;
    });
    private final Duration firstContentTimeout;
    private final Duration totalTimeout;
    private Duration connectTimeout = Duration.ofSeconds(30);
    private Duration readTimeout = Duration.ofMinutes(6);

    // Seed 2.1 Pro 的长总纲实测首正文约 11 分钟、完整响应约 15 分钟。
    public AtlasDeadlineHttpClientBuilder() { this(Duration.ofMinutes(15), Duration.ofMinutes(30)); }

    public AtlasDeadlineHttpClientBuilder(Duration firstContentTimeout, Duration totalTimeout) {
        if (firstContentTimeout == null || totalTimeout == null || firstContentTimeout.isNegative()
            || firstContentTimeout.isZero() || totalTimeout.compareTo(firstContentTimeout) < 0)
            throw new IllegalArgumentException("Invalid Atlas stream deadlines");
        this.firstContentTimeout = firstContentTimeout;
        this.totalTimeout = totalTimeout;
    }

    @Override public Duration connectTimeout() { return connectTimeout; }
    @Override public AtlasDeadlineHttpClientBuilder connectTimeout(Duration timeout) { connectTimeout = timeout; return this; }
    @Override public Duration readTimeout() { return readTimeout; }
    @Override public AtlasDeadlineHttpClientBuilder readTimeout(Duration timeout) { readTimeout = timeout; return this; }

    @Override public HttpClient build() {
        Duration connection = connectTimeout == null || connectTimeout.compareTo(Duration.ofSeconds(30)) > 0
            ? Duration.ofSeconds(30) : connectTimeout;
        var delegate = java.net.http.HttpClient.newBuilder().connectTimeout(connection).build();
        var synchronous = JdkHttpClient.builder().connectTimeout(connection).readTimeout(readTimeout).build();
        return new HttpClient() {
            @Override public SuccessfulHttpResponse execute(HttpRequest request) { return synchronous.execute(request); }

            @Override public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
                var call = new StreamCall(listener);
                call.firstDeadline = DEADLINES.schedule(() -> {
                    if (!call.hasContent.get()) call.fail(new TimeoutException("Atlas 首段正文等待超过配置时限（" + firstContentTimeout.toSeconds() + "秒）"));
                }, firstContentTimeout.toMillis(), TimeUnit.MILLISECONDS);
                call.totalDeadline = DEADLINES.schedule(() -> call.fail(new TimeoutException("Atlas 流式请求超过配置时限（" + totalTimeout.toSeconds() + "秒）")),
                    totalTimeout.toMillis(), TimeUnit.MILLISECONDS);
                try {
                    if (!request.formDataFiles().isEmpty() || !request.formDataFields().isEmpty())
                        throw new IllegalArgumentException("Atlas chat streaming requires a JSON request");
                    var builder = java.net.http.HttpRequest.newBuilder(URI.create(request.url())).timeout(firstContentTimeout);
                    request.headers().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
                    builder.method(request.method().name(), request.body() == null
                        ? java.net.http.HttpRequest.BodyPublishers.noBody()
                        : java.net.http.HttpRequest.BodyPublishers.ofString(request.body()));
                    CompletableFuture<HttpResponse<InputStream>> future = delegate.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
                    call.future.set(future);
                    if (call.terminal.get()) future.cancel(true);
                    future.whenCompleteAsync((response, error) -> {
                        if (error != null) { call.fail(error); return; }
                        call.body.set(response.body());
                        if (call.terminal.get()) { call.closeBody(); return; }
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            call.fail(new HttpException(response.statusCode(), "Atlas HTTP " + response.statusCode())); return;
                        }
                        try (InputStream body = response.body()) {
                            listener.onOpen(SuccessfulHttpResponse.builder().statusCode(response.statusCode())
                                .headers(response.headers().map()).build());
                            parser.parse(body, new ServerSentEventListener() {
                                @Override public void onEvent(ServerSentEvent event, ServerSentEventContext context) {
                                    if (call.terminal.get()) return;
                                    if (hasTextContent(event.data())) {
                                        call.hasContent.set(true); call.firstDeadline.cancel(false);
                                    }
                                    listener.onEvent(event, context);
                                    if ("[DONE]".equals(event.data())) call.complete();
                                }
                                @Override public void onError(Throwable error) { call.fail(error); }
                            });
                            call.complete();
                        } catch (Exception failure) { call.fail(failure); }
                    });
                } catch (Exception failure) { call.fail(failure); }
            }
        };
    }

    static boolean hasTextContent(String data) {
        try {
            for (var choice : JSON.readTree(data).path("choices")) {
                var content = choice.path("delta").path("content");
                if (content.isTextual() && !content.asText().isBlank()) return true;
            }
        } catch (Exception ignored) { }
        return false; // Transport keepalives, role and reasoning deltas are not screenplay text.
    }

    private static final class StreamCall {
        final ServerSentEventListener listener;
        final AtomicBoolean terminal = new AtomicBoolean();
        final AtomicBoolean hasContent = new AtomicBoolean();
        final AtomicReference<CompletableFuture<?>> future = new AtomicReference<>();
        final AtomicReference<InputStream> body = new AtomicReference<>();
        volatile ScheduledFuture<?> firstDeadline;
        volatile ScheduledFuture<?> totalDeadline;
        StreamCall(ServerSentEventListener listener) { this.listener = listener; }
        void fail(Throwable failure) {
            if (!terminal.compareAndSet(false, true)) return;
            cancelDeadlines();
            var pending = future.get(); if (pending != null) pending.cancel(true);
            closeBody();
            listener.onError(failure);
        }
        void complete() {
            if (!terminal.compareAndSet(false, true)) return;
            cancelDeadlines();
            try { listener.onClose(); } finally { closeBody(); }
        }
        void cancelDeadlines() {
            if (firstDeadline != null) firstDeadline.cancel(false);
            if (totalDeadline != null) totalDeadline.cancel(false);
        }
        void closeBody() {
            var stream = body.getAndSet(null);
            if (stream != null) try { stream.close(); } catch (Exception ignored) { }
        }
    }
}
