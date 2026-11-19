package org.ruoyi.chat.service.coding.harness.loop;

import org.ruoyi.chat.service.coding.harness.model.HarnessMessage;
import org.ruoyi.chat.service.coding.harness.model.HarnessOwner;
import org.ruoyi.chat.service.coding.harness.store.HarnessStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.nio.charset.StandardCharsets;

/** Bounded page reader that never assumes a session ledger fits in one store call. */
@Service
public class HarnessTranscriptReader {

    private static final int PAGE_SIZE = 32;
    private static final int MAX_WORKING_MESSAGES = 100_000;
    private static final long MAX_WORKING_BYTES = 32L * 1_024 * 1_024;
    private static final int MAX_STREAMED_MESSAGES = 1_000_000;
    private final HarnessStore store;

    public HarnessTranscriptReader(HarnessStore store) {
        this.store = store;
    }

    public List<HarnessMessage> readAfter(HarnessOwner owner, String sessionId,
                                          long afterSequence) {
        List<HarnessMessage> messages = new ArrayList<>();
        long cursor = afterSequence;
        long bytes = 0;
        while (true) {
            List<HarnessMessage> page = store.readMessages(owner, sessionId, cursor, PAGE_SIZE);
            if (page.isEmpty()) {
                return List.copyOf(messages);
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException("Harness store returned a non-monotonic message page");
                }
                cursor = message.sequence();
                messages.add(message);
                bytes = safeAdd(bytes, estimatedBytes(message));
                if (messages.size() > MAX_WORKING_MESSAGES) {
                    throw new IllegalStateException("Harness working transcript exceeds safety limit");
                }
                if (bytes > MAX_WORKING_BYTES) {
                    throw new IllegalStateException(
                        "Harness working transcript exceeds the 32 MiB materialization limit");
                }
            }
            if (page.size() < PAGE_SIZE) {
                return List.copyOf(messages);
            }
        }
    }

    /** Streams ledger records in bounded pages without retaining the whole session in heap. */
    public void forEachAfter(HarnessOwner owner, String sessionId, long afterSequence,
                             Consumer<HarnessMessage> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("Message consumer is required");
        }
        long cursor = afterSequence;
        int inspected = 0;
        while (true) {
            List<HarnessMessage> page = store.readMessages(owner, sessionId, cursor, PAGE_SIZE);
            if (page.isEmpty()) {
                return;
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException(
                        "Harness store returned a non-monotonic message page");
                }
                cursor = message.sequence();
                if (++inspected > MAX_STREAMED_MESSAGES) {
                    throw new IllegalStateException(
                        "Harness ledger scan exceeds the streamed-message safety limit");
                }
                consumer.accept(message);
            }
            if (page.size() < PAGE_SIZE) {
                return;
            }
        }
    }

    /** Finds one indexed-style fact without constructing a session-sized temporary list. */
    public Optional<HarnessMessage> findFirstAfter(HarnessOwner owner, String sessionId,
                                                    long afterSequence,
                                                    Predicate<HarnessMessage> predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException("Message predicate is required");
        }
        long cursor = afterSequence;
        int inspected = 0;
        while (true) {
            List<HarnessMessage> page = store.readMessages(owner, sessionId, cursor, PAGE_SIZE);
            if (page.isEmpty()) {
                return Optional.empty();
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException(
                        "Harness store returned a non-monotonic message page");
                }
                cursor = message.sequence();
                if (++inspected > MAX_STREAMED_MESSAGES) {
                    throw new IllegalStateException(
                        "Harness ledger scan exceeds the streamed-message safety limit");
                }
                if (predicate.test(message)) {
                    return Optional.of(message);
                }
            }
            if (page.size() < PAGE_SIZE) {
                return Optional.empty();
            }
        }
    }

    private long estimatedBytes(HarnessMessage message) {
        long bytes = utf8(message.content()) + utf8(message.thinking())
            + utf8(message.toolCallId()) + utf8(message.toolName());
        for (var call : message.toolCalls()) {
            bytes = safeAdd(bytes, utf8(call.toolCallId()));
            bytes = safeAdd(bytes, utf8(call.toolName()));
            bytes = safeAdd(bytes, utf8(call.arguments()));
        }
        return safeAdd(bytes, 256);
    }

    private long utf8(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }

    private long safeAdd(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
}
