package org.ruoyi.chat.service.coding.harness.loop.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import org.ruoyi.chat.service.coding.harness.model.HarnessToolCall;
import org.ruoyi.chat.service.coding.harness.tool.ToolCapability;
import org.ruoyi.chat.service.coding.harness.tool.ToolDescriptor;
import org.ruoyi.chat.service.coding.harness.tool.ToolInvocation;
import org.ruoyi.chat.service.coding.harness.tool.builtin.BuiltinToolException;
import org.ruoyi.chat.service.coding.harness.tool.command.CommandToolException;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Exact registry shared by prompt construction, provider schemas, policy and execution. A tool
 * absent from this registry cannot be advertised or invoked.
 */
public final class HarnessToolRegistry {

    private static final TypeReference<Map<String, Object>> ARGUMENT_MAP = new TypeReference<>() { };

    private final ObjectMapper objectMapper;
    private final Map<String, HarnessToolRegistration> registrations;

    private HarnessToolRegistry(ObjectMapper objectMapper,
                                Map<String, HarnessToolRegistration> registrations) {
        this.objectMapper = objectMapper.copy()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.registrations = Map.copyOf(registrations);
    }

    public static Builder builder(ObjectMapper objectMapper) {
        return new Builder(objectMapper);
    }

    public List<ToolSpecification> specifications() {
        return registrations.values().stream()
            .map(HarnessToolRegistration::specification)
            .sorted(Comparator.comparing(ToolSpecification::name))
            .toList();
    }

    public List<ToolDescriptor> descriptors() {
        return registrations.values().stream()
            .map(HarnessToolRegistration::descriptor)
            .sorted(Comparator.comparing(ToolDescriptor::toolName))
            .toList();
    }

    public Optional<ToolDescriptor> descriptor(String name) {
        HarnessToolRegistration registration = registrations.get(key(name));
        return registration == null ? Optional.empty() : Optional.of(registration.descriptor());
    }

    /** Returns a registry containing only explicitly named existing registrations. */
    public HarnessToolRegistry restrictedTo(Set<String> names) {
        Objects.requireNonNull(names, "names");
        Set<String> normalized = names.stream().map(HarnessToolRegistry::key)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<String, HarnessToolRegistration> selected = new LinkedHashMap<>();
        registrations.forEach((name, registration) -> {
            if (normalized.contains(name)) {
                selected.put(name, registration);
            }
        });
        return selected.size() == registrations.size()
            ? this : new HarnessToolRegistry(objectMapper, selected);
    }

    /** Parses one complete JSON object and derives absolute mutation targets for policy checks. */
    public PreparedToolCall prepare(HarnessToolCall call, Path workspace) {
        Objects.requireNonNull(call, "call");
        Path root = Objects.requireNonNull(workspace, "workspace").toAbsolutePath().normalize();
        HarnessToolRegistration registration = registrations.get(key(call.toolName()));
        if (registration == null) {
            throw new IllegalArgumentException("Unknown Harness tool: " + call.toolName());
        }
        byte[] input = call.arguments().getBytes(StandardCharsets.UTF_8);
        if (input.length > registration.descriptor().maxInputBytes()) {
            throw new IllegalArgumentException("Tool arguments exceed the registered input limit");
        }
        Map<String, Object> arguments = parseArguments(call.arguments());
        List<String> targets = mutationTargets(registration.descriptor(), arguments, root);
        ToolInvocation invocation = new ToolInvocation(call.toolCallId(), call.toolName(),
            operation(arguments, call.toolName()), arguments, targets);
        return new PreparedToolCall(call, invocation, registration.descriptor());
    }

    /** Invokes the exact annotated method; callers own timeout, offload and persistence policy. */
    public HarnessToolExecutionResult execute(HarnessToolCall call) {
        Objects.requireNonNull(call, "call");
        HarnessToolRegistration registration = registrations.get(key(call.toolName()));
        if (registration == null) {
            return failure(call, "unknown_tool", "Unknown Harness tool: " + call.toolName(), 0);
        }
        long started = System.nanoTime();
        try {
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id(call.toolCallId())
                .name(registration.specification().name())
                .arguments(call.arguments())
                .build();
            String content;
            try (HarnessToolInvocationContext.Scope ignored =
                     HarnessToolInvocationContext.open(call)) {
                content = registration.executor().execute(request, null);
            }
            content = content == null ? "null" : content;
            if (content.getBytes(StandardCharsets.UTF_8).length
                > registration.descriptor().maxOutputBytes()) {
                return failure(call, "tool_output_too_large",
                    "Tool output exceeds the registered output limit", elapsed(started));
            }
            if (Set.of("execute_process", "run_inline_probe").contains(key(call.toolName()))) {
                return processResult(call, content, elapsed(started));
            }
            return new HarnessToolExecutionResult(call.toolCallId(), call.toolName(), false,
                "ok", content, elapsed(started));
        } catch (RuntimeException error) {
            Throwable cause = unwrap(error);
            if (cause instanceof BuiltinToolException builtin) {
                return failure(call, builtin.code(), builtin.getMessage(), elapsed(started));
            }
            if (cause instanceof CommandToolException command) {
                return failure(call, command.code(), command.getMessage(), elapsed(started));
            }
            return failure(call, "tool_execution_failed", safeMessage(cause), elapsed(started));
        }
    }

    private Map<String, Object> parseArguments(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException("Tool arguments must be one JSON object");
            }
            return objectMapper.convertValue(node, ARGUMENT_MAP);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("Tool arguments are not valid JSON", error);
        }
    }

    private List<String> mutationTargets(ToolDescriptor descriptor, Map<String, Object> arguments,
                                         Path root) {
        if (!descriptor.hasCapability(ToolCapability.WRITE)
            && !descriptor.hasCapability(ToolCapability.DESTRUCTIVE)) {
            return List.of();
        }
        List<String> rawTargets = new ArrayList<>();
        Object path = arguments.get("path");
        if (path instanceof String value && !value.isBlank()) {
            rawTargets.add(value);
        }
        Object paths = arguments.get("paths");
        if (paths instanceof Collection<?> values) {
            for (Object value : values) {
                if (value instanceof String text && !text.isBlank()) {
                    rawTargets.add(text);
                }
            }
        }
        return rawTargets.stream().map(value -> {
            Path candidate = Path.of(value);
            return (candidate.isAbsolute() ? candidate : root.resolve(candidate))
                .toAbsolutePath().normalize().toString();
        }).toList();
    }

    private static String operation(Map<String, Object> arguments, String fallback) {
        Object operation = arguments.get("operation");
        return operation instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static HarnessToolExecutionResult failure(HarnessToolCall call, String code,
                                                       String message, long duration) {
        return new HarnessToolExecutionResult(call.toolCallId(), call.toolName(), true,
            code == null || code.isBlank() ? "tool_execution_failed" : code,
            message == null || message.isBlank() ? "Tool execution failed" : message,
            duration);
    }

    /**
     * A process invocation can execute normally while the program itself fails. LangChain4j
     * serializes {@code ProcessExecutionResult} as JSON, so classify that structured outcome here
     * without replacing its stdout/stderr payload. Malformed process output is also an error: a
     * caller must never receive an unverifiable process result marked successful.
     */
    private HarnessToolExecutionResult processResult(HarnessToolCall call, String content,
                                                      long duration) {
        try {
            JsonNode result = objectMapper.readTree(content);
            JsonNode exitCode = result == null ? null : result.get("exitCode");
            JsonNode timedOut = result == null ? null : result.get("timedOut");
            if (result == null || !result.isObject() || exitCode == null
                || !exitCode.isIntegralNumber() || !exitCode.canConvertToInt()
                || timedOut == null || !timedOut.isBoolean()) {
                return processFailure(call, "INVALID_PROCESS_RESULT", content, duration);
            }
            if (timedOut.booleanValue()) {
                return processFailure(call, "run_inline_probe".equals(key(call.toolName()))
                    ? "INLINE_PROBE_TIMEOUT" : "PROCESS_TIMEOUT", content, duration);
            }
            if (exitCode.intValue() != 0) {
                return processFailure(call, "run_inline_probe".equals(key(call.toolName()))
                    ? "INLINE_PROBE_COUNTEREVIDENCE" : "PROCESS_EXIT_NONZERO", content,
                    duration);
            }
            return new HarnessToolExecutionResult(call.toolCallId(), call.toolName(), false,
                "PROCESS_EXIT_ZERO", content, duration);
        } catch (JsonProcessingException error) {
            return processFailure(call, "INVALID_PROCESS_RESULT", content, duration);
        }
    }

    private static HarnessToolExecutionResult processFailure(HarnessToolCall call, String code,
                                                              String content, long duration) {
        return new HarnessToolExecutionResult(call.toolCallId(), call.toolName(), true, code,
            content, duration);
    }

    private static long elapsed(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000L);
    }

    private static Throwable unwrap(Throwable error) {
        for (Throwable candidate = error; candidate != null;
             candidate = candidate.getCause()) {
            if (candidate instanceof BuiltinToolException
                || candidate instanceof CommandToolException) {
                return candidate;
            }
        }
        Throwable current = error;
        for (int depth = 0; depth < 16 && current.getCause() != null; depth++) {
            if (!isInvocationWrapper(current)) {
                break;
            }
            current = current.getCause();
        }
        return current;
    }

    private static boolean isInvocationWrapper(Throwable error) {
        String type = error.getClass().getName();
        return error instanceof java.lang.reflect.InvocationTargetException
            || error instanceof java.lang.reflect.UndeclaredThrowableException
            || error instanceof java.util.concurrent.CompletionException
            || error instanceof java.util.concurrent.ExecutionException
            || type.equals("dev.langchain4j.exception.ToolExecutionException");
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
            ? error.getClass().getSimpleName() : message;
    }

    private static String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static final class Builder {
        private final ObjectMapper objectMapper;
        private final Map<String, HarnessToolRegistration> registrations = new LinkedHashMap<>();

        private Builder(ObjectMapper objectMapper) {
            this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        }

        public Builder registerAnnotated(Object toolObject,
                                         Collection<ToolDescriptor> descriptors) {
            return registerAnnotated(toolObject, descriptors, true);
        }

        /** Registers an explicit phase-safe subset while still rejecting unmatched descriptors. */
        public Builder registerAnnotatedSubset(Object toolObject,
                                               Collection<ToolDescriptor> descriptors) {
            return registerAnnotated(toolObject, descriptors, false);
        }

        private Builder registerAnnotated(Object toolObject,
                                          Collection<ToolDescriptor> descriptors,
                                          boolean requireEveryAnnotatedMethod) {
            Objects.requireNonNull(toolObject, "toolObject");
            Map<String, ToolDescriptor> byName = new LinkedHashMap<>();
            for (ToolDescriptor descriptor : descriptors) {
                if (byName.putIfAbsent(key(descriptor.toolName()), descriptor) != null) {
                    throw new IllegalArgumentException("Duplicate descriptor: " + descriptor.toolName());
                }
            }
            for (Method method : toolObject.getClass().getMethods()) {
                if (!method.isAnnotationPresent(Tool.class)) {
                    continue;
                }
                ToolSpecification specification = ToolSpecifications.toolSpecificationFrom(method);
                ToolDescriptor descriptor = byName.remove(key(specification.name()));
                if (descriptor == null) {
                    if (requireEveryAnnotatedMethod) {
                        throw new IllegalArgumentException("Missing descriptor for tool: "
                            + specification.name());
                    }
                    continue;
                }
                register(new HarnessToolRegistration(specification, descriptor,
                    DefaultToolExecutor.builder()
                        .object(toolObject)
                        .originalMethod(method)
                        .methodToInvoke(method)
                        .wrapToolArgumentsExceptions(true)
                        .propagateToolExecutionExceptions(true)
                        .build()));
            }
            if (!byName.isEmpty()) {
                throw new IllegalArgumentException("Descriptors have no annotated method: "
                    + byName.keySet());
            }
            return this;
        }

        private void register(HarnessToolRegistration registration) {
            String name = key(registration.specification().name());
            if (registrations.putIfAbsent(name, registration) != null) {
                throw new IllegalArgumentException("Duplicate Harness tool: " + name);
            }
        }

        public HarnessToolRegistry build() {
            return new HarnessToolRegistry(objectMapper, registrations);
        }
    }
}
