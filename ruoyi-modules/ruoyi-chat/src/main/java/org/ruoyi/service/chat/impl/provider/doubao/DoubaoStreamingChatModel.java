package org.ruoyi.service.chat.impl.provider.doubao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.chat.request.json.JsonAnyOfSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNullSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonReferenceSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialThinkingContext;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.PartialToolCallContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.StreamingHandle;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * 字节 Doubao-Seed-Evolving 专用流式 Chat Model（OpenAI Chat Completions 兼容协议）。
 *
 * <p>为什么不用 LangChain4j 自带的 OpenAI 模型：SDK 原生在请求侧会忽略 assistant 消息的
 * {@code encrypted_content}，在响应侧也无法捕获该字段。而 Doubao 工具多轮上下文要求把
 * 上一轮 assistant 消息的思考加密原文原样回传。本桥接：</p>
 * <ul>
 *   <li>请求侧：发送 {@code reasoning_effort}（七档）与 {@code thinking={type,...}}；
 *       把 {@link AiMessage#attributes()} 中携带的 encrypted_content 与 reasoning_content
 *       原样放入 assistant 消息；图片 detail 支持 low/high/xhigh（xhigh 真实发送，不降级）；
 *       工具参数由 SDK 的 JsonObjectSchema/JsonArraySchema 等显式转换为真实 JSON Schema。</li>
 *   <li>响应侧：解析 SSE 分片，输出 content/reasoning_content 增量、工具调用分片，
 *       捕获最后一段非空 {@code encrypted_content} 放入最终 AiMessage 的 attributes；
 *       正确组装 finishReason 与 usage。</li>
 *   <li>支持取消（StreamingHandle）、读取阶段截止时间与 HTTP 非 2xx 错误
 *       （读取响应体用于分类，但不记录密钥/密文）。终态回调防并发与重复，
 *       并清理活动 SSE 流与计时任务，避免泄漏。</li>
 * </ul>
 */
public class DoubaoStreamingChatModel implements StreamingChatModel {

    /** AiMessage.attributes() 中存储 Doubao 思考加密原文的键。 */
    public static final String ENCRYPTED_CONTENT_ATTRIBUTE = "doubao_encrypted_content";
    /** 与 attributes 对应的请求 JSON 字段名。 */
    public static final String ENCRYPTED_CONTENT_FIELD = "encrypted_content";
    private static final String REASONING_CONTENT_FIELD = "reasoning_content";
    /** 非 2xx 响应最多读取的错误体字节数（仅用于错误分类，不包含请求密钥）。 */
    private static final int MAX_ERROR_BODY_BYTES = 4_096;
    /**
     * 单轮流式截止时间默认值（10 分钟）：与 DurableHarnessRunProcessor
     * 的 coding.harness.model-timeout.millis 默认 600000ms（10 分钟）协调，
     * 避免 high/max/xhigh 等长思考回合尚未到达 Harness 总控制上限就被接入层提前切断。
     */
    static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);
    /** 单轮截止时间上限（30 分钟）：超出明确拒绝，防止无界的不受控调度。 */
    static final Duration MAX_TIMEOUT = Duration.ofMinutes(30);

    /** 守护线程调度器：为 SSE 读取阶段提供超时/取消支持，不阻止 JVM 退出。 */
    private static final ScheduledExecutorService TIMEOUT_SCHEDULER =
        Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "doubao-stream-timeout");
            thread.setDaemon(true);
            return thread;
        });

    private final String endpoint;
    private final String apiKey;
    private final String modelName;
    private final Duration timeout;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String reasoningEffort;
    private final boolean thinkingEnabled;

    private DoubaoStreamingChatModel(Builder builder) {
        this.endpoint = requireNotBlank(builder.endpoint, "endpoint");
        this.apiKey = requireNotBlank(builder.apiKey, "apiKey");
        this.modelName = requireNotBlank(builder.modelName, "modelName");
        this.timeout = resolveTimeout(builder.timeout);
        this.objectMapper = builder.objectMapper == null
            ? new ObjectMapper() : builder.objectMapper;
        this.reasoningEffort = builder.reasoningEffort;
        this.thinkingEnabled = builder.thinkingEnabled;
        // 不跟随重定向：Authorization 头携带密钥，跟随 302 会把凭据带到第三方主机。
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(builder.connectTimeout == null
                ? Duration.ofSeconds(30) : builder.connectTimeout)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        ObjectNode requestBody;
        try {
            requestBody = serializeRequest(chatRequest);
        } catch (RuntimeException failure) {
            handler.onError(failure);
            return;
        }
        HttpRequest httpRequest;
        try {
            httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();
        } catch (Exception failure) {
            handler.onError(new IllegalStateException(
                "无法构造 Doubao 请求（不包含密钥信息）", failure));
            return;
        }

        StreamState state = new StreamState(handler);
        try {
            CompletableFuture<HttpResponse<Stream<String>>> responseFuture =
                httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines());
            state.bindFuture(responseFuture);
            // 截止时间必须覆盖 SSE body 读取阶段：响应头到达后连接仍可能长时间挂起。
            // 只有成功、失败或取消等终态才会清理该计时任务。
            ScheduledFuture<?> deadline = TIMEOUT_SCHEDULER.schedule(state::checkIdle,
                timeout.toNanos(), TimeUnit.NANOSECONDS);
            state.bindDeadline(deadline);
            responseFuture.whenComplete((response, error) -> {
                if (error != null) {
                    // CompletableFuture.cancel(true) 以 CancellationException 完成；
                    // sendAsync 的失败包装为 CompletionException，统一解包。
                    state.fail(unwrap(error));
                    return;
                }
                int statusCode = response.statusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    state.fail(new IllegalStateException(
                        "Doubao 返回 HTTP " + statusCode + "（模型或网关拒绝了请求）"
                            + errorBodyExcerpt(response)));
                    return;
                }
                // 活动流交给句柄管理：取消/超时会真正关闭它，中断仍在读取的 body。
                state.bindStream(response.body());
                try (Stream<String> lines = response.body()) {
                    lines.forEach(state::acceptLine);
                    state.finish();
                } catch (RuntimeException failure) {
                    // 流被 cancel/超时关闭时会在这里抛出；fail 对终态幂等。
                    state.fail(unwrap(failure));
                }
            });
        } catch (RuntimeException failure) {
            state.fail(failure);
        }
    }

    /** 读取非 2xx 响应体的一小段用于错误分类；任何异常都退化为空摘要，绝不包含密钥。 */
    private static String errorBodyExcerpt(HttpResponse<Stream<String>> response) {
        try (Stream<String> lines = response.body()) {
            StringBuilder excerpt = new StringBuilder();
            lines.takeWhile(line -> excerpt.length() < MAX_ERROR_BODY_BYTES).forEach(line -> {
                if (excerpt.length() < MAX_ERROR_BODY_BYTES) {
                    excerpt.append(line).append('\n');
                }
            });
            String text = excerpt.toString().strip();
            if (text.isEmpty()) {
                return "";
            }
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_ERROR_BODY_BYTES) {
                text = new String(bytes, 0, MAX_ERROR_BODY_BYTES, StandardCharsets.UTF_8) + "…";
            }
            return "：" + text;
        } catch (RuntimeException ignored) {
            // 错误体无法读取时不掩盖原始 HTTP 失败。
            return "";
        }
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        if (current instanceof ExecutionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private ObjectNode serializeRequest(ChatRequest chatRequest) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", modelName);
        body.put("stream", true);
        body.put("stream_options", objectMapper.createObjectNode().put("include_usage", true));

        ObjectNode thinking = body.putObject("thinking");
        thinking.put("type", thinkingEnabled ? "enabled" : "disabled");
        // none 关闭思考：thinking.type=disabled，且不发送 reasoning_effort。
        if (thinkingEnabled && reasoningEffort != null && !reasoningEffort.isBlank()) {
            body.put("reasoning_effort", reasoningEffort);
        }

        ArrayNode messages = body.putArray("messages");
        for (ChatMessage message : chatRequest.messages()) {
            messages.add(serializeMessage(message));
        }

        ChatRequestParameters parameters = chatRequest.parameters();
        if (parameters != null) {
            if (parameters.maxOutputTokens() != null) {
                body.put("max_tokens", parameters.maxOutputTokens());
            }
            if (parameters.toolSpecifications() != null && !parameters.toolSpecifications().isEmpty()) {
                ArrayNode tools = body.putArray("tools");
                for (ToolSpecification specification : parameters.toolSpecifications()) {
                    ObjectNode tool = tools.addObject();
                    ObjectNode function = tool.putObject("function");
                    function.put("name", specification.name());
                    function.put("description", specification.description() == null
                        ? "" : specification.description());
                    // SDK 的 Json*Schema 不是标准 JavaBean，必须显式生成真实 JSON Schema，
                    // 覆盖嵌套对象、数组、枚举、必填项与 additionalProperties。
                    function.set("parameters", specification.parameters() == null
                        ? emptyObjectSchema() : schemaToJsonNode(specification.parameters()));
                    tool.put("type", "function");
                }
            }
            ToolChoice toolChoice = parameters.toolChoice();
            if (toolChoice == ToolChoice.REQUIRED) {
                // 当前配置接入的 Doubao 接口实测：显式 tool_choice=required 必然被
                // HTTP 400 invalid request params 拒绝。正常路径由
                // HarnessChatModelFactory 的能力声明（requiredToolChoiceSupported=false）
                // 保证不会传入 REQUIRED；若上层误传，直接在本地明确报错，
                // 不悄悄承诺强制调用，也不发出已知会被拒绝的 HTTP 请求。
                throw new UnsupportedOperationException(
                    "Doubao 当前接入接口不支持 forced/required tool choice"
                        + "（tool_choice=required 会被服务端拒绝），"
                        + "请省略 tool_choice 以使用模型默认工具选择");
            }
            // AUTO（与未设置）使用协议默认行为：省略 tool_choice 字段。
            // 实测显式 tool_choice=auto 同样被当前接口 HTTP 400 拒绝，而省略字段时
            // 模型在有工具场景下仍正常返回工具调用。
        }
        return body;
    }

    private ObjectNode emptyObjectSchema() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "object");
        node.set("properties", objectMapper.createObjectNode());
        return node;
    }

    /**
     * 把 LangChain4j 的 {@link JsonSchemaElement} 显式转换为 JSON Schema 节点。
     * SDK 类不携带可自动序列化的 type 字段，因此按各子类的真实访问器逐字段输出。
     */
    private ObjectNode schemaToJsonNode(JsonSchemaElement schema) {
        ObjectNode node = objectMapper.createObjectNode();
        if (schema == null) {
            node.put("type", "object");
            return node;
        }
        if (schema instanceof JsonObjectSchema objectSchema) {
            node.put("type", "object");
            ObjectNode properties = node.putObject("properties");
            Map<String, JsonSchemaElement> schemaProperties = objectSchema.properties();
            if (schemaProperties != null) {
                schemaProperties.forEach((name, property) ->
                    properties.set(name, schemaToJsonNode(property)));
            }
            List<String> required = objectSchema.required();
            if (required != null && !required.isEmpty()) {
                ArrayNode requiredNode = node.putArray("required");
                required.forEach(requiredNode::add);
            }
            Boolean additionalProperties = objectSchema.additionalProperties();
            if (additionalProperties != null) {
                node.put("additionalProperties", additionalProperties);
            }
            Map<String, JsonSchemaElement> definitions = objectSchema.definitions();
            if (definitions != null && !definitions.isEmpty()) {
                ObjectNode definitionsNode = node.putObject("$defs");
                definitions.forEach((name, definition) ->
                    definitionsNode.set(name, schemaToJsonNode(definition)));
            }
        } else if (schema instanceof JsonArraySchema arraySchema) {
            node.put("type", "array");
            if (arraySchema.items() != null) {
                node.set("items", schemaToJsonNode(arraySchema.items()));
            }
        } else if (schema instanceof JsonEnumSchema enumSchema) {
            node.put("type", "string");
            if (enumSchema.enumValues() != null) {
                ArrayNode enumNode = node.putArray("enum");
                enumSchema.enumValues().forEach(enumNode::add);
            }
        } else if (schema instanceof JsonStringSchema) {
            node.put("type", "string");
        } else if (schema instanceof JsonIntegerSchema) {
            node.put("type", "integer");
        } else if (schema instanceof JsonNumberSchema) {
            node.put("type", "number");
        } else if (schema instanceof JsonBooleanSchema) {
            node.put("type", "boolean");
        } else if (schema instanceof JsonNullSchema) {
            node.put("type", "null");
        } else if (schema instanceof JsonAnyOfSchema anyOfSchema) {
            ArrayNode anyOfNode = node.putArray("anyOf");
            if (anyOfSchema.anyOf() != null) {
                anyOfSchema.anyOf().forEach(option -> anyOfNode.add(schemaToJsonNode(option)));
            }
        } else if (schema instanceof JsonReferenceSchema referenceSchema) {
            if (referenceSchema.reference() != null) {
                node.put("$ref", referenceSchema.reference());
            }
        } else if (schema instanceof JsonRawSchema rawSchema) {
            try {
                return (ObjectNode) objectMapper.readTree(rawSchema.schema());
            } catch (Exception failure) {
                throw new IllegalArgumentException(
                    "Doubao 工具参数包含无法解析的原生 JSON Schema", failure);
            }
        } else {
            // 未知/未建模的 schema 元素退化为宽松对象，而不是输出缺少 type 的无效结构。
            node.put("type", "object");
        }
        if (schema != null && schema.description() != null && !schema.description().isBlank()) {
            node.put("description", schema.description());
        }
        return node;
    }

    private ObjectNode serializeMessage(ChatMessage message) {
        ObjectNode node = objectMapper.createObjectNode();
        switch (message.type()) {
            case SYSTEM -> {
                node.put("role", "system");
                node.put("content", ((SystemMessage) message).text());
            }
            case USER -> {
                node.put("role", "user");
                UserMessage userMessage = (UserMessage) message;
                ArrayNode content = node.putArray("content");
                for (Content item : userMessage.contents()) {
                    ObjectNode part = content.addObject();
                    if (item.type() == ContentType.IMAGE) {
                        ImageContent image = (ImageContent) item;
                        part.put("type", "image_url");
                        ObjectNode imageUrl = part.putObject("image_url");
                        imageUrl.put("url", "data:" + image.image().mimeType()
                            + ";base64," + image.image().base64Data());
                        imageUrl.put("detail", doubaoImageDetail(image.detailLevel()));
                    } else {
                        part.put("type", "text");
                        part.put("text", ((TextContent) item).text());
                    }
                }
            }
            case AI -> {
                node.put("role", "assistant");
                AiMessage aiMessage = (AiMessage) message;
                if (aiMessage.text() != null) {
                    node.put("content", aiMessage.text());
                }
                if (aiMessage.thinking() != null && !aiMessage.thinking().isBlank()) {
                    node.put(REASONING_CONTENT_FIELD, aiMessage.thinking());
                }
                Object encrypted = aiMessage.attributes() == null
                    ? null : aiMessage.attributes().get(ENCRYPTED_CONTENT_ATTRIBUTE);
                if (encrypted instanceof String encryptedText && !encryptedText.isBlank()) {
                    // Doubao 工具多轮上下文：思考加密原文必须原样回传；永不展示到 UI/日志。
                    node.put(ENCRYPTED_CONTENT_FIELD, encryptedText);
                }
                if (aiMessage.toolExecutionRequests() != null
                    && !aiMessage.toolExecutionRequests().isEmpty()) {
                    ArrayNode toolCalls = node.putArray("tool_calls");
                    int index = 0;
                    for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                        ObjectNode call = toolCalls.addObject();
                        call.put("index", index++);
                        call.put("id", request.id());
                        call.put("type", "function");
                        call.putObject("function")
                            .put("name", request.name())
                            .put("arguments", request.arguments() == null ? "{}" : request.arguments());
                    }
                }
            }
            case TOOL_EXECUTION_RESULT -> {
                ToolExecutionResultMessage result = (ToolExecutionResultMessage) message;
                node.put("role", "tool");
                node.put("tool_call_id", result.id());
                node.put("content", result.text());
            }
            default -> throw new IllegalArgumentException(
                "Doubao 桥接不支持的消息类型: " + message.type());
        }
        return node;
    }

    /**
     * LangChain4j 图片枚举到 Doubao detail 字面值。ULTRA_HIGH 真实映射为 xhigh，
     * MEDIUM 归入 high，LOW 为 low，AUTO/HIGH 为 high（Doubao 默认 high）。
     */
    private static String doubaoImageDetail(ImageContent.DetailLevel detailLevel) {
        if (detailLevel == null) {
            return "high";
        }
        return switch (detailLevel) {
            case LOW -> "low";
            case ULTRA_HIGH -> "xhigh";
            case MEDIUM, HIGH, AUTO -> "high";
        };
    }

    private static FinishReason finishReason(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "stop" -> FinishReason.STOP;
            case "length" -> FinishReason.LENGTH;
            case "tool_calls", "function_call" -> FinishReason.TOOL_EXECUTION;
            case "content_filter" -> FinishReason.CONTENT_FILTER;
            default -> FinishReason.OTHER;
        };
    }

    /** 解析 SSE 并聚合最终响应；字段缺失/分片不产生虚构事件。 */
    private final class StreamState {
        private final StreamingChatResponseHandler handler;
        private final AbortableStreamingHandle streamingHandle =
            new AbortableStreamingHandle();
        private final StringBuilder content = new StringBuilder();
        private final StringBuilder reasoning = new StringBuilder();
        private final StringBuilder encryptedContent = new StringBuilder();
        private final Map<Integer, ToolCallAccumulator> toolCalls = new LinkedHashMap<>();
        /** 终态闸门：成功/失败/取消只允许一个观察者通过，回调与清理只发生一次。 */
        private final AtomicBoolean terminal = new AtomicBoolean(false);
        private String responseId;
        private String responseModel;
        private String finishReason;
        private TokenUsage tokenUsage;
        private volatile long lastProgressNanos = System.nanoTime();

        private void checkIdle() {
            if (terminal.get() || streamingHandle.isCancelled()) {
                return;
            }
            long remaining = timeout.toNanos() - (System.nanoTime() - lastProgressNanos);
            if (remaining > 0) {
                bindDeadline(TIMEOUT_SCHEDULER.schedule(this::checkIdle, remaining, TimeUnit.NANOSECONDS));
            } else if (markTerminal()) {
                abortAfterTerminal();
                handler.onError(new TimeoutException("Doubao 响应持续无数据，等待超时"));
            }
        }

        private StreamState(StreamingChatResponseHandler handler) {
            this.handler = handler;
        }

        private void bindFuture(CompletableFuture<HttpResponse<Stream<String>>> future) {
            streamingHandle.bindFuture(future);
        }

        private void bindStream(Stream<String> stream) {
            streamingHandle.bindStream(stream);
        }

        private void bindDeadline(ScheduledFuture<?> deadline) {
            streamingHandle.bindDeadline(deadline);
        }

        /** CAS 进入终态；返回 true 的调用方负责终态回调。 */
        private boolean markTerminal() {
            return terminal.compareAndSet(false, true);
        }

        /** 成功终态：仅停止截止计时，活动流由 try-with-resources 正常关闭。 */
        private void settleAfterSuccess() {
            streamingHandle.settle();
        }

        /** 失败/超时/取消终态：停止计时并真正中止活动 SSE 流。 */
        private void abortAfterTerminal() {
            streamingHandle.cancel();
        }

        private void acceptLine(String rawLine) {
            if (rawLine == null || terminal.get()) {
                return;
            }
            String line = rawLine.strip();
            if (line.isEmpty() || !line.startsWith("data:")) {
                return;
            }
            String data = line.substring("data:".length()).strip();
            lastProgressNanos = System.nanoTime();
            if ("[DONE]".equals(data)) {
                return;
            }
            try {
                JsonNode chunk = objectMapper.readTree(data);
                parseChunk(chunk);
            } catch (Exception parseFailure) {
                // readTree 声明受检 JsonProcessingException；单个分片解析失败不伪造内容，
                // 明确报错，交由上层重试/失败处理。
                fail(new IllegalStateException("Doubao SSE 分片解析失败", parseFailure));
            }
        }

        private void parseChunk(JsonNode chunk) {
            JsonNode errorNode = chunk.get("error");
            if (errorNode != null && errorNode.isObject()) {
                // 例如 {"error":{"code":"rate_limit_exceeded",...}}：不能当成成功完成。
                String code = errorNode.path("code").asText("provider_error");
                String message = errorNode.path("message").asText("");
                fail(new IllegalStateException("Doubao 返回错误事件 " + code
                    + (message.isBlank() ? "" : ": " + message)));
                return;
            }
            if (chunk.hasNonNull("id")) {
                responseId = chunk.get("id").asText();
            }
            if (chunk.hasNonNull("model")) {
                responseModel = chunk.get("model").asText();
            }
            JsonNode usage = chunk.get("usage");
            if (usage != null && usage.isObject()) {
                Integer prompt = usage.hasNonNull("prompt_tokens")
                    ? usage.get("prompt_tokens").asInt() : null;
                Integer completion = usage.hasNonNull("completion_tokens")
                    ? usage.get("completion_tokens").asInt() : null;
                Integer total = usage.hasNonNull("total_tokens")
                    ? usage.get("total_tokens").asInt() : null;
                tokenUsage = new TokenUsage(prompt, completion, total);
            }
            JsonNode choices = chunk.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                return;
            }
            JsonNode choice = choices.get(0);
            JsonNode finish = choice.get("finish_reason");
            if (finish != null && !finish.isNull()) {
                finishReason = finish.asText();
            }
            JsonNode delta = choice.get("delta");
            if (delta == null || !delta.isObject()) {
                return;
            }
            if (delta.hasNonNull("content")) {
                String text = delta.get("content").asText();
                if (!text.isEmpty()) {
                    content.append(text);
                    // 必须使用携带上下文的新回调：只调旧 String 回调时，
                    // 通过新回调注册 StreamingHandle 的调用方拿不到取消句柄。
                    handler.onPartialResponse(new PartialResponse(text),
                        new PartialResponseContext(streamingHandle));
                }
            }
            if (delta.hasNonNull(REASONING_CONTENT_FIELD)) {
                String thinkingText = delta.get(REASONING_CONTENT_FIELD).asText();
                if (!thinkingText.isEmpty()) {
                    reasoning.append(thinkingText);
                    handler.onPartialThinking(new PartialThinking(thinkingText),
                        new PartialThinkingContext(streamingHandle));
                }
            }
            if (delta.hasNonNull(ENCRYPTED_CONTENT_FIELD)) {
                String encrypted = delta.get(ENCRYPTED_CONTENT_FIELD).asText();
                if (!encrypted.isEmpty()) {
                    // 加密原文在同一轮可能分片到达，按顺序累积，永不展示到 UI/日志。
                    encryptedContent.append(encrypted);
                }
            }
            JsonNode toolCallNodes = delta.get("tool_calls");
            if (toolCallNodes != null && toolCallNodes.isArray()) {
                for (JsonNode toolCall : toolCallNodes) {
                    int index = toolCall.hasNonNull("index")
                        ? toolCall.get("index").asInt() : toolCalls.size();
                    ToolCallAccumulator accumulator = toolCalls.computeIfAbsent(index,
                        ignored -> new ToolCallAccumulator());
                    if (toolCall.hasNonNull("id")) {
                        accumulator.id.append(toolCall.get("id").asText());
                    }
                    JsonNode function = toolCall.get("function");
                    if (function != null && function.isObject()) {
                        if (function.hasNonNull("name")) {
                            accumulator.name.append(function.get("name").asText());
                        }
                        if (function.hasNonNull("arguments")) {
                            String argumentsFragment = function.get("arguments").asText();
                            accumulator.arguments.append(argumentsFragment);
                            emitPartialToolCall(index, accumulator, argumentsFragment);
                        }
                    }
                }
            }
        }

        private void emitPartialToolCall(int index, ToolCallAccumulator accumulator,
                                         String argumentsFragment) {
            String id = accumulator.id.toString();
            String name = accumulator.name.toString();
            // PartialToolCall 构造器要求 name 非空白、arguments 非空；
            // 名称尚未到达的首分片先跳过（最终聚合不受影响）。
            if (name.isBlank() || argumentsFragment.isEmpty()) {
                return;
            }
            PartialToolCall.Builder builder = PartialToolCall.builder()
                .index(index)
                .name(name)
                .partialArguments(argumentsFragment);
            if (!id.isBlank()) {
                builder.id(id);
            }
            handler.onPartialToolCall(builder.build(),
                new PartialToolCallContext(streamingHandle));
        }

        private void finish() {
            if (!markTerminal()) {
                return;
            }
            // 没有 finishReason 说明流在完成前被截断（未收到结束分片），不能当作成功完成。
            if (finishReason == null || finishReason.isBlank()) {
                abortAfterTerminal();
                handler.onError(new IllegalStateException(
                    "Doubao 流式响应在结束前被截断（缺少 finish_reason）"));
                return;
            }
            settleAfterSuccess();
            List<ToolExecutionRequest> requests = new ArrayList<>();
            toolCalls.keySet().stream().sorted().forEach(index -> {
                ToolCallAccumulator accumulator = toolCalls.get(index);
                String id = accumulator.id.toString();
                String name = accumulator.name.toString();
                String arguments = accumulator.arguments.toString();
                if (id.isEmpty() && name.isEmpty() && arguments.isEmpty()) {
                    return;
                }
                requests.add(ToolExecutionRequest.builder()
                    .id(id.isBlank() ? null : id)
                    .name(name.isBlank() ? null : name)
                    .arguments(arguments.isBlank() ? "{}" : arguments)
                    .build());
            });
            AiMessage.Builder messageBuilder = AiMessage.builder()
                .text(content.length() == 0 ? null : content.toString())
                .thinking(reasoning.length() == 0 ? null : reasoning.toString());
            Map<String, Object> attributes = new LinkedHashMap<>();
            if (encryptedContent.length() > 0) {
                attributes.put(ENCRYPTED_CONTENT_ATTRIBUTE, encryptedContent.toString());
            }
            messageBuilder.toolExecutionRequests(requests);
            if (!attributes.isEmpty()) {
                messageBuilder.attributes(attributes);
            }
            ChatResponse.Builder responseBuilder = ChatResponse.builder()
                .aiMessage(messageBuilder.build());
            if (responseId != null) {
                responseBuilder.id(responseId);
            }
            if (responseModel != null) {
                responseBuilder.modelName(responseModel);
            }
            if (tokenUsage != null) {
                responseBuilder.tokenUsage(tokenUsage);
            }
            FinishReason mapped = finishReason(finishReason);
            if (mapped != null) {
                responseBuilder.finishReason(mapped);
            }
            handler.onCompleteResponse(responseBuilder.build());
        }

        private void fail(Throwable failure) {
            if (failure == null) {
                failure = new IllegalStateException("Doubao 流式请求失败");
            }
            if (!markTerminal()) {
                return;
            }
            abortAfterTerminal();
            Throwable cause = unwrap(failure);
            handler.onError(cause instanceof RuntimeException runtime
                ? runtime : new RuntimeException(cause));
        }
    }

    private static final class ToolCallAccumulator {
        private final StringBuilder id = new StringBuilder();
        private final StringBuilder name = new StringBuilder();
        private final StringBuilder arguments = new StringBuilder();
    }

    /**
     * 取消句柄：取消底层 JDK HttpClient 异步请求并关闭活动 SSE 流。
     * 仅取消已返回响应头的 future 无法停止 body 读取，因此同时关闭活动流。
     */
    private final class AbortableStreamingHandle implements StreamingHandle {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile CompletableFuture<?> future;
        private volatile Stream<String> activeStream;
        private volatile ScheduledFuture<?> deadline;

        private void bindFuture(CompletableFuture<?> future) {
            this.future = future;
        }

        private void bindStream(Stream<String> stream) {
            this.activeStream = stream;
            if (cancelled.get()) {
                closeStream();
            }
        }

        private void bindDeadline(ScheduledFuture<?> deadline) {
            this.deadline = deadline;
            if (cancelled.get()) {
                deadline.cancel(false);
            }
        }

        /**
         * 成功终态结算：只停止截止计时。不设置取消标记（{@link #isCancelled()} 仍为 false），
         * 也不中止活动流——流由读取方的 try-with-resources 正常关闭。
         */
        private void settle() {
            ScheduledFuture<?> scheduled = deadline;
            if (scheduled != null) {
                scheduled.cancel(false);
            }
        }

        @Override
        public void cancel() {
            if (!cancelled.compareAndSet(false, true)) {
                return;
            }
            settle();
            CompletableFuture<?> current = future;
            if (current != null) {
                current.cancel(true);
            }
            closeStream();
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        private void closeStream() {
            Stream<String> stream = activeStream;
            if (stream != null) {
                try {
                    stream.close();
                } catch (RuntimeException ignored) {
                    // 关闭活动流以中断读取；关闭异常不影响取消语义。
                }
            }
        }
    }

    /**
     * 解析并校验单轮流式截止时间：未指定时取默认 10 分钟；必须为正数且不超过 30 分钟。
     * 零/负数会让 HTTP 请求与调度计时行为不受控，缺失上限会造成无界等待，均在构造期明确拒绝。
     */
    private static Duration resolveTimeout(Duration timeout) {
        Duration resolved = timeout == null ? DEFAULT_TIMEOUT : timeout;
        if (resolved.isZero() || resolved.isNegative()) {
            throw new IllegalArgumentException(
                "Doubao 流式超时必须为正数（timeout=" + resolved + "）");
        }
        if (resolved.compareTo(MAX_TIMEOUT) > 0) {
            throw new IllegalArgumentException(
                "Doubao 流式超时不得超过 " + MAX_TIMEOUT + "（timeout=" + resolved + "）");
        }
        return resolved;
    }

    private static String requireNotBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return DefaultChatRequestParameters.EMPTY;
    }

    public static final class Builder {
        private String endpoint;
        private String apiKey;
        private String modelName;
        private Duration timeout;
        private Duration connectTimeout;
        private ObjectMapper objectMapper;
        private String reasoningEffort;
        private boolean thinkingEnabled = true;

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        /**
         * 单轮流式截止时间；未设置时默认 10 分钟（与 Harness 单轮模型超时默认值协调）。
         * 必须为正数且不超过 30 分钟，否则 {@link #build()} 抛出 IllegalArgumentException；
         * 仍允许显式设置较短的截止时间（如本地协议验收场景）。
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        /** Doubao reasoning_effort 字面值（none 时请同时 {@link #thinkingEnabled(boolean)} 为 false）。 */
        public Builder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        public Builder thinkingEnabled(boolean thinkingEnabled) {
            this.thinkingEnabled = thinkingEnabled;
            return this;
        }

        public DoubaoStreamingChatModel build() {
            return new DoubaoStreamingChatModel(this);
        }
    }
}
