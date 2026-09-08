package org.ruoyi.service.coding.harness.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.ruoyi.service.coding.harness.model.HarnessEvent;
import org.ruoyi.service.coding.harness.model.HarnessMessage;
import org.ruoyi.service.coding.harness.model.HarnessOwner;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;
import org.ruoyi.service.coding.harness.modelruntime.HarnessModelRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/**
 * Lightweight single-instance durable store. Snapshots are atomically replaced and ledgers are
 * append-only. The interface deliberately permits a database implementation for clustered use.
 */
@Repository
public class FileHarnessStore implements HarnessStore {

    private static final int MAX_READ_LIMIT = 10_000;
    private static final int MAX_RECOVERY_SCAN_LIMIT = 1_000;
    private static final String SAFE_ID = "[A-Za-z0-9_-]{1,128}";
    private static final String RECOVERY_CURSOR =
        "[A-Za-z0-9_-]{1,128}/[1-9][0-9]*/[A-Za-z0-9_-]{1,128}/[A-Za-z0-9_-]{1,128}";
    private static final ReentrantLock[] PROCESS_LEDGER_LOCKS = createLedgerLockStripes(256);
    private static final HarnessModelRouter LEGACY_MODEL_ROUTER = new HarnessModelRouter();

    private final ObjectMapper objectMapper;
    private final Path dataRoot;
    private final ConcurrentMap<String, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> messageSequences = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, EventLedgerCursor> eventSequences = new ConcurrentHashMap<>();
    private final LedgerAppendFaultInjector ledgerAppendFaultInjector;

    @Autowired
    public FileHarnessStore(ObjectMapper objectMapper,
                            @Value("${coding.harness.data-dir:./data/coding-harness}") String dataDirectory) {
        // Keep state snapshots isolated from artifact-store housekeeping directories. Recovery
        // intentionally treats every child of this root as an owner namespace and must never
        // interpret `.staging` or `artifacts/owners` as persisted sessions.
        this(objectMapper, Path.of(dataDirectory).resolve("state"));
    }

    public FileHarnessStore(ObjectMapper objectMapper, Path dataRoot) {
        this(objectMapper, dataRoot, LedgerAppendFaultInjector.NONE);
    }

    FileHarnessStore(ObjectMapper objectMapper, Path dataRoot,
                     LedgerAppendFaultInjector ledgerAppendFaultInjector) {
        this.objectMapper = objectMapper.copy()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.dataRoot = dataRoot.toAbsolutePath().normalize();
        this.ledgerAppendFaultInjector = ledgerAppendFaultInjector == null
            ? LedgerAppendFaultInjector.NONE : ledgerAppendFaultInjector;
        try {
            Files.createDirectories(this.dataRoot);
        } catch (IOException e) {
            throw new HarnessStoreException("Cannot create Harness data directory " + this.dataRoot, e);
        }
    }

    @Override
    public HarnessSessionState createSession(HarnessSessionState session) {
        verifyOwner(session.owner(), new HarnessOwner(session.tenantId(), session.userId()));
        requireSafeId("sessionId", session.sessionId());
        return withSessionLock(session.owner(), session.sessionId(), () -> {
            Path file = sessionFile(session.owner(), session.sessionId());
            if (Files.exists(file)) {
                throw new HarnessStoreException("Session already exists: " + session.sessionId());
            }
            atomicWrite(file, session);
            return session;
        });
    }

    @Override
    public Optional<HarnessSessionState> findSession(HarnessOwner owner, String sessionId) {
        requireSafeId("sessionId", sessionId);
        return withSessionLock(owner, sessionId, () -> readOptional(
            sessionFile(owner, sessionId), HarnessSessionState.class).map(state -> {
                verifyOwner(owner, state.owner());
                return state;
            }));
    }

    @Override
    public List<HarnessSessionState> listSessions(HarnessOwner owner) {
        Path ownerRoot = ownerRoot(owner);
        if (!Files.isDirectory(ownerRoot)) {
            return List.of();
        }
        try (var directories = Files.list(ownerRoot)) {
            return directories.filter(Files::isDirectory)
                .map(path -> path.resolve("session.json"))
                .filter(Files::isRegularFile)
                .map(path -> read(path, HarnessSessionState.class))
                .peek(state -> verifyOwner(owner, state.owner()))
                .filter(state -> state.deletedAt() == 0)
                .sorted(Comparator.comparingLong(HarnessSessionState::pinnedAt).reversed()
                    .thenComparing(Comparator.comparingLong(HarnessSessionState::updatedAt).reversed()))
                .toList();
        } catch (IOException e) {
            throw new HarnessStoreException("Cannot list Harness sessions", e);
        }
    }

    @Override
    public HarnessSessionState setSessionPinned(HarnessOwner owner, String sessionId, boolean pinned) {
        requireSafeId("sessionId", sessionId);
        return withSessionLock(owner, sessionId, () -> {
            Path file = sessionFile(owner, sessionId);
            HarnessSessionState current = readRequired(file, HarnessSessionState.class, "session");
            verifyOwner(owner, current.owner());
            if (current.deletedAt() != 0) {
                throw new HarnessStoreException("会话已删除，请先恢复会话");
            }
            long pinnedAt = pinned ? (current.pinnedAt() == 0
                ? System.currentTimeMillis() : current.pinnedAt()) : 0;
            HarnessSessionState updated = current.withSidebarState(pinnedAt, 0);
            atomicWrite(file, updated);
            return updated;
        });
    }

    @Override
    public HarnessSessionState setSessionDeleted(HarnessOwner owner, String sessionId, boolean deleted) {
        requireSafeId("sessionId", sessionId);
        return withSessionLock(owner, sessionId, () -> {
            Path file = sessionFile(owner, sessionId);
            HarnessSessionState current = readRequired(file, HarnessSessionState.class, "session");
            verifyOwner(owner, current.owner());
            if (deleted && current.deletedAt() == 0
                && listRuns(owner, sessionId).stream().anyMatch(run -> !run.status().isTerminal())) {
                throw new HarnessStoreException("该会话还有未结束的任务，请先停止任务再删除会话");
            }
            long deletedAt = deleted ? (current.deletedAt() == 0
                ? System.currentTimeMillis() : current.deletedAt()) : 0;
            // Keep the pin position so undo restores the complete sidebar state.
            HarnessSessionState updated = current.withSidebarState(current.pinnedAt(), deletedAt);
            atomicWrite(file, updated);
            return updated;
        });
    }

    @Override
    public HarnessSessionState saveSession(HarnessOwner owner, HarnessSessionState session,
                                           long expectedRevision) {
        requireSafeId("sessionId", session.sessionId());
        verifyOwner(owner, session.owner());
        return withSessionLock(owner, session.sessionId(), () -> {
            Path file = sessionFile(owner, session.sessionId());
            HarnessSessionState current = readRequired(file, HarnessSessionState.class, "session");
            verifyOwner(owner, current.owner());
            if (current.revision() != expectedRevision || session.revision() != expectedRevision) {
                throw new HarnessOptimisticLockException("session " + session.sessionId(),
                    expectedRevision, current.revision());
            }
            if (!current.workspace().equals(session.workspace())) {
                throw new HarnessStoreException("A session workspace lease is immutable");
            }
            if (!current.workspaceManifest().equals(session.workspaceManifest())) {
                throw new HarnessStoreException("A session workspace manifest is immutable");
            }
            // 会话的思考等级、验证归属在创建后即固定，后续 withTitle/withActiveRun 等更新
            // 必须原样保留；改动视为冲突并拒绝。模型同样固定。
            if (!java.util.Objects.equals(current.model(), session.model())
                || current.thinkingLevel() != session.thinkingLevel()
                || current.verificationMode() != session.verificationMode()) {
                throw new HarnessStoreException(
                    "A session model, thinking level and verification mode are immutable");
            }
            // An execution snapshot may predate a pin/delete action. Preserve the latest sidebar
            // state without making UI-only changes conflict with an in-flight agent update.
            HarnessSessionState stored = session.withSidebarState(current.pinnedAt(), current.deletedAt())
                .withRevision(expectedRevision + 1);
            atomicWrite(file, stored);
            return stored;
        });
    }

    @Override
    public HarnessRunState createRun(HarnessOwner owner, HarnessRunState run) {
        requireSafeId("sessionId", run.sessionId());
        requireSafeId("runId", run.runId());
        verifyOwner(owner, run.owner());
        return withSessionLock(owner, run.sessionId(), () -> {
            HarnessSessionState session = readRequired(sessionFile(owner, run.sessionId()),
                HarnessSessionState.class, "session");
            verifyOwner(owner, session.owner());
            if (session.deletedAt() != 0) {
                throw new HarnessStoreException("会话已删除，请先恢复会话");
            }
            if (run.modelRoute() == null
                || !session.model().equals(run.modelRoute().requestedModel())) {
                throw new HarnessStoreException("Run model route does not belong to its session");
            }
            Path file = runFile(owner, run.sessionId(), run.runId());
            if (Files.exists(file)) {
                throw new HarnessStoreException("Run already exists: " + run.runId());
            }
            atomicWrite(file, run);
            return run;
        });
    }

    @Override
    public Optional<HarnessRunState> findRun(HarnessOwner owner, String sessionId, String runId) {
        requireSafeId("sessionId", sessionId);
        requireSafeId("runId", runId);
        return withSessionLock(owner, sessionId, () -> {
            HarnessSessionState session = readOptional(sessionFile(owner, sessionId),
                HarnessSessionState.class).orElse(null);
            if (session == null) {
                return Optional.empty();
            }
            verifyOwner(owner, session.owner());
            return readOptional(runFile(owner, sessionId, runId), HarnessRunState.class)
                .map(state -> recoverLegacyModelRoute(state, session)).map(state -> {
                verifyOwner(owner, state.owner());
                return state;
            });
        });
    }

    @Override
    public List<HarnessRunState> listRuns(HarnessOwner owner, String sessionId) {
        requireSafeId("sessionId", sessionId);
        return withSessionLock(owner, sessionId, () -> {
            HarnessSessionState session = readRequired(sessionFile(owner, sessionId),
                HarnessSessionState.class, "session");
            verifyOwner(owner, session.owner());
            Path runs = sessionDirectory(owner, sessionId).resolve("runs");
            if (!Files.isDirectory(runs)) {
                return List.of();
            }
            try (var directories = Files.list(runs)) {
                return directories.filter(Files::isDirectory)
                    .map(path -> path.resolve("state.json"))
                    .filter(Files::isRegularFile)
                    .map(path -> recoverLegacyModelRoute(read(path, HarnessRunState.class), session))
                    .peek(state -> verifyOwner(owner, state.owner()))
                    .sorted(Comparator.comparingLong(HarnessRunState::createdAt))
                    .toList();
            } catch (IOException e) {
                throw new HarnessStoreException("Cannot list runs for session " + sessionId, e);
            }
        });
    }

    @Override
    public HarnessRunState saveRun(HarnessOwner owner, HarnessRunState run, long expectedRevision) {
        requireSafeId("sessionId", run.sessionId());
        requireSafeId("runId", run.runId());
        verifyOwner(owner, run.owner());
        return withSessionLock(owner, run.sessionId(), () ->
            saveRunUnderSessionLock(owner, run, expectedRevision));
    }

    @Override
    public HarnessRunState saveRunIfMessageLedgerUnchanged(
        HarnessOwner owner, HarnessRunState run, long expectedRevision,
        long expectedMessageSequence
    ) {
        requireSafeId("sessionId", run.sessionId());
        requireSafeId("runId", run.runId());
        verifyOwner(owner, run.owner());
        if (expectedMessageSequence < 0) {
            throw new IllegalArgumentException("Message ledger high-watermark cannot be negative");
        }
        return withSessionLock(owner, run.sessionId(), () -> {
            long actualSequence = maxMessageSequence(messagesFile(owner, run.sessionId()));
            if (actualSequence != expectedMessageSequence) {
                throw new HarnessMessageLedgerConflictException(run.sessionId(),
                    expectedMessageSequence, actualSequence);
            }
            return saveRunUnderSessionLock(owner, run, expectedRevision);
        });
    }

    private HarnessRunState saveRunUnderSessionLock(HarnessOwner owner, HarnessRunState run,
                                                     long expectedRevision) {
        Path file = runFile(owner, run.sessionId(), run.runId());
        HarnessSessionState session = readRequired(sessionFile(owner, run.sessionId()),
            HarnessSessionState.class, "session");
        verifyOwner(owner, session.owner());
        HarnessRunState current = recoverLegacyModelRoute(
            readRequired(file, HarnessRunState.class, "run"), session);
        HarnessRunState candidate = recoverLegacyModelRoute(run, session);
        verifyOwner(owner, current.owner());
        if (current.revision() != expectedRevision || candidate.revision() != expectedRevision) {
            throw new HarnessOptimisticLockException("run " + run.runId(), expectedRevision,
                current.revision());
        }
        if (!current.originalRequirement().equals(candidate.originalRequirement())
            || current.permissionMode() != candidate.permissionMode()
            || current.permissionRevision() != candidate.permissionRevision()
            || !Objects.equals(current.modelRoute(), candidate.modelRoute())
            || !session.model().equals(candidate.modelRoute().requestedModel())) {
            throw new HarnessStoreException("Run anchors are immutable");
        }
        HarnessRunState stored = candidate.withRevision(expectedRevision + 1);
        atomicWrite(file, stored);
        return stored;
    }

    @Override
    public HarnessRunScanPage scanRunsForRecovery(String afterCursor, int limit) {
        if (limit < 1 || limit > MAX_RECOVERY_SCAN_LIMIT) {
            throw new IllegalArgumentException("Recovery scan limit must be between 1 and "
                + MAX_RECOVERY_SCAN_LIMIT);
        }
        if (afterCursor != null && !afterCursor.matches(RECOVERY_CURSOR)) {
            throw new IllegalArgumentException("Invalid recovery scan cursor");
        }
        if (!Files.isDirectory(dataRoot, LinkOption.NOFOLLOW_LINKS)) {
            return new HarnessRunScanPage(List.of(), null);
        }

        List<RecoveryRunLocation> locations = new ArrayList<>(limit + 1);
        scanRecoveryLocations(afterCursor, limit + 1, locations);
        boolean hasMore = locations.size() > limit;
        List<RecoveryRunLocation> pageLocations = hasMore
            ? locations.subList(0, limit) : locations;
        List<HarnessRunState> runs = pageLocations.stream()
            .map(this::readAndVerifyRecoveryRun)
            .toList();
        String nextCursor = hasMore ? pageLocations.get(pageLocations.size() - 1).cursor() : null;
        return new HarnessRunScanPage(runs, nextCursor);
    }

    @Override
    public HarnessMessage appendMessage(HarnessOwner owner, HarnessMessage message) {
        requireSafeId("sessionId", message.sessionId());
        requireSafeId("runId", message.runId());
        return withSessionLock(owner, message.sessionId(), () -> {
            requireRun(owner, message.sessionId(), message.runId());
            Path ledger = messagesFile(owner, message.sessionId());
            String key = sessionKey(owner, message.sessionId());
            long sequence = messageSequences.computeIfAbsent(key,
                ignored -> new AtomicLong(maxMessageSequence(ledger))).incrementAndGet();
            HarnessMessage stored = message.withSequence(sequence);
            appendJsonLine(ledger, stored);
            return stored;
        });
    }

    @Override
    public List<HarnessMessage> readMessages(HarnessOwner owner, String sessionId,
                                             long afterSequence, int limit) {
        validateRead(afterSequence, limit);
        requireSafeId("sessionId", sessionId);
        return withSessionLock(owner, sessionId, () -> {
            HarnessSessionState session = readRequired(sessionFile(owner, sessionId),
                HarnessSessionState.class, "session");
            verifyOwner(owner, session.owner());
            return readLedgerPage(messagesFile(owner, sessionId), HarnessMessage.class,
                HarnessMessage::sequence, afterSequence, limit);
        });
    }

    @Override
    public HarnessEvent appendEvent(HarnessOwner owner, HarnessEvent event) {
        requireSafeId("sessionId", event.sessionId());
        requireSafeId("runId", event.runId());
        return withSessionLock(owner, event.sessionId(), () -> {
            requireRun(owner, event.sessionId(), event.runId());
            Path ledger = eventsFile(owner, event.sessionId(), event.runId());
            String key = sessionKey(owner, event.sessionId()) + "/" + event.runId();
            return withLedgerFileLock(ledger, () -> appendEventUnderLedgerLock(key, ledger, event));
        });
    }

    @Override
    public List<HarnessEvent> readEvents(HarnessOwner owner, String sessionId, String runId,
                                         long afterSequence, int limit) {
        validateRead(afterSequence, limit);
        requireSafeId("sessionId", sessionId);
        requireSafeId("runId", runId);
        return withSessionLock(owner, sessionId, () -> {
            requireRun(owner, sessionId, runId);
            return readLedgerPage(eventsFile(owner, sessionId, runId), HarnessEvent.class,
                HarnessEvent::sequence, afterSequence, limit);
        });
    }

    private void requireRun(HarnessOwner owner, String sessionId, String runId) {
        HarnessRunState run = readRequired(runFile(owner, sessionId, runId), HarnessRunState.class, "run");
        verifyOwner(owner, run.owner());
    }

    private void scanRecoveryLocations(String afterCursor, int target,
                                       List<RecoveryRunLocation> locations) {
        for (Path tenantDirectory : sortedDirectories(dataRoot, "tenant")) {
            String tenantId = tenantDirectory.getFileName().toString();
            requireSafeId("tenantId", tenantId);
            for (Path userDirectory : sortedDirectories(tenantDirectory, "user")) {
                String userSegment = userDirectory.getFileName().toString();
                long userId = parseRecoveryUserId(userSegment);
                HarnessOwner owner = new HarnessOwner(tenantId, userId);
                for (Path sessionDirectory : sortedDirectories(userDirectory, "session")) {
                    String sessionId = sessionDirectory.getFileName().toString();
                    requireSafeId("sessionId", sessionId);
                    Path runsDirectory = sessionDirectory.resolve("runs");
                    if (!Files.exists(runsDirectory, LinkOption.NOFOLLOW_LINKS)) {
                        continue;
                    }
                    requireRealDirectory(runsDirectory, "runs");
                    for (Path runDirectory : sortedDirectories(runsDirectory, "run")) {
                        String runId = runDirectory.getFileName().toString();
                        requireSafeId("runId", runId);
                        String cursor = tenantId + "/" + userSegment + "/" + sessionId + "/" + runId;
                        if (afterCursor != null && cursor.compareTo(afterCursor) <= 0) {
                            continue;
                        }
                        Path stateFile = runDirectory.resolve("state.json");
                        if (!Files.exists(stateFile, LinkOption.NOFOLLOW_LINKS)) {
                            continue;
                        }
                        requireRegularFile(stateFile, "run state");
                        Path sessionFile = sessionDirectory.resolve("session.json");
                        requireRegularFile(sessionFile, "session state");
                        locations.add(new RecoveryRunLocation(cursor, owner, sessionId, runId));
                        if (locations.size() == target) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private HarnessRunState readAndVerifyRecoveryRun(RecoveryRunLocation location) {
        HarnessSessionState session;
        HarnessRunState run;
        try {
            session = findSession(location.owner(), location.sessionId())
                .orElseThrow(() -> new HarnessStoreException(
                    "Recovery scan found a run without its session: " + location.cursor()));
            run = findRun(location.owner(), location.sessionId(), location.runId())
                .orElseThrow(() -> new HarnessStoreException(
                    "Recovery run disappeared while scanning: " + location.cursor()));
        } catch (HarnessStoreException mismatch) {
            throw new HarnessStoreException(
                "Recovery snapshot does not match its physical owner path: " + location.cursor(),
                mismatch);
        }
        if (!location.sessionId().equals(session.sessionId())
            || !location.owner().equals(session.owner())) {
            throw new HarnessStoreException(
                "Recovery session snapshot does not match its physical owner path");
        }
        if (!location.runId().equals(run.runId())
            || !location.sessionId().equals(run.sessionId())
            || !location.owner().equals(run.owner())) {
            throw new HarnessStoreException(
                "Recovery run snapshot does not match its physical owner path");
        }
        return run;
    }

    private HarnessRunState recoverLegacyModelRoute(HarnessRunState run,
                                                    HarnessSessionState session) {
        if (run.modelRoute() != null) {
            return run;
        }
        // Doubao 旧快照恢复时固定 Doubao，并沿用会话思考等级；DeepSeek 保持原有回退策略。
        org.ruoyi.service.coding.harness.model.HarnessModelRoute route =
            org.ruoyi.service.coding.harness.modelruntime.HarnessModelPolicy.isDoubao(session.model())
                ? LEGACY_MODEL_ROUTER.route(session, run.originalRequirement(), false)
                : LEGACY_MODEL_ROUTER.legacySessionFallback(
                    session.model(), run.originalRequirement());
        return run.withRecoveredModelRoute(new org.ruoyi.service.coding.harness.model.HarnessModelRoute(
            route.policyVersion(), route.requestedModel(), route.selectedModel(),
            route.taskClass(), route.thinkingEnabled(),
            org.ruoyi.service.coding.harness.model.HarnessModelRouteSource.LEGACY_SESSION_FALLBACK));
    }

    private List<Path> sortedDirectories(Path parent, String label) {
        requireRealDirectory(parent, label + " parent");
        try (var children = Files.list(parent)) {
            List<Path> directories = children
                .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
            // A symbolic link is neither accepted nor silently followed during global recovery.
            try (var entries = Files.list(parent)) {
                Optional<Path> symbolicLink = entries.filter(Files::isSymbolicLink).findFirst();
                if (symbolicLink.isPresent()) {
                    throw new HarnessStoreException("Symbolic link is forbidden in recovery storage: "
                        + symbolicLink.get());
                }
            }
            return directories;
        } catch (IOException e) {
            throw new HarnessStoreException("Cannot enumerate Harness recovery " + label
                + " directories", e);
        }
    }

    private long parseRecoveryUserId(String value) {
        try {
            long userId = Long.parseLong(value);
            if (userId <= 0 || !Long.toString(userId).equals(value)) {
                throw new NumberFormatException("non-canonical user id");
            }
            return userId;
        } catch (NumberFormatException invalid) {
            throw new HarnessStoreException("Invalid user directory in recovery storage: " + value,
                invalid);
        }
    }

    private void requireRealDirectory(Path path, String label) {
        if (Files.isSymbolicLink(path)
            || !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new HarnessStoreException("Invalid " + label + " directory in recovery storage: "
                + path);
        }
        requireInsideRecoveryRoot(path, label);
    }

    private void requireRegularFile(Path path, String label) {
        if (Files.isSymbolicLink(path)
            || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new HarnessStoreException("Invalid " + label + " in recovery storage: " + path);
        }
        requireInsideRecoveryRoot(path, label);
    }

    private void requireInsideRecoveryRoot(Path path, String label) {
        try {
            Path canonicalRoot = dataRoot.toRealPath();
            Path canonicalPath = path.toRealPath();
            if (!canonicalPath.startsWith(canonicalRoot)) {
                throw new HarnessStoreException("Recovery " + label
                    + " resolves outside the Harness data root: " + path);
            }
        } catch (IOException error) {
            throw new HarnessStoreException("Cannot resolve recovery " + label + ": " + path,
                error);
        }
    }

    private record RecoveryRunLocation(String cursor, HarnessOwner owner,
                                       String sessionId, String runId) {
    }

    private record EventLedgerCursor(long sequence, long byteLength) {
    }

    @FunctionalInterface
    interface LedgerAppendFaultInjector {

        LedgerAppendFaultInjector NONE = (file, originalLength, bytesWritten) -> {
        };

        void afterWrite(Path file, long originalLength, long bytesWritten) throws IOException;
    }

    private HarnessEvent appendEventUnderLedgerLock(String key, Path ledger, HarnessEvent event) {
        try {
            Files.createDirectories(ledger.getParent());
            repairTornTail(ledger);
            long byteLength = Files.exists(ledger) ? Files.size(ledger) : 0L;
            EventLedgerCursor cached = eventSequences.get(key);
            long durableSequence = cached != null && cached.byteLength() == byteLength
                ? cached.sequence() : maxEventSequence(ledger);
            if (durableSequence == Long.MAX_VALUE) {
                throw new HarnessStoreException("Harness event sequence is exhausted for " + ledger);
            }
            long sequence = durableSequence + 1;
            HarnessEvent stored = event.withSequence(sequence);
            long committedLength = appendJsonLineTransactionally(ledger, stored, byteLength);
            // The cache is only advanced after the full record has reached durable storage. A
            // failed append therefore reuses the same candidate sequence on the next attempt.
            eventSequences.put(key, new EventLedgerCursor(sequence, committedLength));
            return stored;
        } catch (IOException error) {
            throw new HarnessStoreException("Cannot append Harness event ledger " + ledger, error);
        }
    }

    private <T> T withLedgerFileLock(Path ledger, Supplier<T> operation) {
        Path lockFile = ledger.resolveSibling(ledger.getFileName() + ".lock")
            .toAbsolutePath().normalize();
        ReentrantLock processLock = PROCESS_LEDGER_LOCKS[Math.floorMod(lockFile.hashCode(),
            PROCESS_LEDGER_LOCKS.length)];
        processLock.lock();
        try {
            Files.createDirectories(lockFile.getParent());
            try (FileChannel lockChannel = FileChannel.open(lockFile, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
                 FileLock ignored = lockChannel.lock()) {
                return operation.get();
            }
        } catch (IOException error) {
            throw new HarnessStoreException("Cannot lock Harness event ledger " + ledger, error);
        } finally {
            processLock.unlock();
        }
    }

    private long appendJsonLineTransactionally(Path file, Object value, long originalLength) {
        long bytesWritten = 0L;
        boolean appendStarted = false;
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            ByteBuffer buffer = ByteBuffer.allocate(json.length + 1);
            buffer.put(json).put((byte) '\n').flip();
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE)) {
                if (channel.size() != originalLength) {
                    throw new IOException("Harness event ledger changed while exclusively locked");
                }
                channel.position(originalLength);
                while (buffer.hasRemaining()) {
                    appendStarted = true;
                    int written = channel.write(buffer);
                    if (written < 0) {
                        throw new IOException("Unexpected end while appending Harness event ledger");
                    }
                    if (written == 0) {
                        continue;
                    }
                    bytesWritten += written;
                    ledgerAppendFaultInjector.afterWrite(file, originalLength, bytesWritten);
                }
                channel.force(true);
                long committedLength = originalLength + json.length + 1L;
                if (channel.size() != committedLength) {
                    throw new IOException("Harness event ledger append length mismatch");
                }
                return committedLength;
            }
        } catch (IOException | RuntimeException failure) {
            if (appendStarted) {
                try {
                    rollbackLedgerAppend(file, originalLength);
                } catch (IOException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
            throw new HarnessStoreException("Cannot atomically append Harness ledger " + file,
                failure);
        }
    }

    private void rollbackLedgerAppend(Path file, long originalLength) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            if (channel.size() > originalLength) {
                channel.truncate(originalLength);
                channel.force(true);
            }
        }
    }

    private static ReentrantLock[] createLedgerLockStripes(int count) {
        ReentrantLock[] locks = new ReentrantLock[count];
        for (int index = 0; index < count; index++) {
            locks[index] = new ReentrantLock();
        }
        return locks;
    }

    private long maxMessageSequence(Path ledger) {
        return maxLedgerSequence(ledger, HarnessMessage.class, HarnessMessage::sequence);
    }

    private long maxEventSequence(Path ledger) {
        return maxLedgerSequence(ledger, HarnessEvent.class, HarnessEvent::sequence);
    }

    private <T> List<T> readLedgerPage(Path file, Class<T> type,
                                       ToLongFunction<T> sequenceExtractor,
                                       long afterSequence, int limit) {
        if (!Files.exists(file)) {
            return List.of();
        }
        boolean terminated = fileEndsWithNewline(file);
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<T> records = new ArrayList<>(Math.min(limit, 1_000));
            String line;
            long lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    T record = objectMapper.readValue(line, type);
                    if (sequenceExtractor.applyAsLong(record) > afterSequence) {
                        records.add(record);
                        if (records.size() == limit) {
                            break;
                        }
                    }
                } catch (IOException parseError) {
                    if (!terminated && !hasNonBlankRemainder(reader)) {
                        break;
                    }
                    throw new HarnessStoreException("Corrupt Harness ledger at " + file
                        + " line " + lineNumber, parseError);
                }
            }
            return records;
        } catch (IOException e) {
            throw new HarnessStoreException("Cannot read Harness ledger " + file, e);
        }
    }

    private <T> long maxLedgerSequence(Path file, Class<T> type,
                                       ToLongFunction<T> sequenceExtractor) {
        if (!Files.exists(file)) {
            return 0;
        }
        boolean terminated = fileEndsWithNewline(file);
        long maximum = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            long lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    maximum = Math.max(maximum,
                        sequenceExtractor.applyAsLong(objectMapper.readValue(line, type)));
                } catch (IOException parseError) {
                    if (!terminated && !hasNonBlankRemainder(reader)) {
                        break;
                    }
                    throw new HarnessStoreException("Corrupt Harness ledger at " + file
                        + " line " + lineNumber, parseError);
                }
            }
            return maximum;
        } catch (IOException e) {
            throw new HarnessStoreException("Cannot scan Harness ledger " + file, e);
        }
    }

    private boolean hasNonBlankRemainder(BufferedReader reader) throws IOException {
        String remainder;
        while ((remainder = reader.readLine()) != null) {
            if (!remainder.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private boolean fileEndsWithNewline(Path file) {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            if (channel.size() == 0) {
                return true;
            }
            ByteBuffer last = ByteBuffer.allocate(1);
            channel.position(channel.size() - 1);
            channel.read(last);
            last.flip();
            return last.hasRemaining() && last.get() == (byte) '\n';
        } catch (IOException e) {
            throw new HarnessStoreException("Cannot inspect Harness ledger " + file, e);
        }
    }

    private void appendJsonLine(Path file, Object value) {
        try {
            Files.createDirectories(file.getParent());
            repairTornTail(file);
            byte[] json = objectMapper.writeValueAsBytes(value);
            ByteBuffer buffer = ByteBuffer.allocate(json.length + 1);
            buffer.put(json).put((byte) '\n').flip();
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(false);
            }
        } catch (IOException e) {
            throw new HarnessStoreException("Cannot append Harness ledger " + file, e);
        }
    }

    private void repairTornTail(Path file) throws IOException {
        if (!Files.exists(file) || Files.size(file) == 0 || fileEndsWithNewline(file)) {
            return;
        }
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ,
            StandardOpenOption.WRITE)) {
            long position = channel.size() - 1;
            ByteBuffer byteBuffer = ByteBuffer.allocate(1);
            while (position >= 0) {
                byteBuffer.clear();
                channel.position(position);
                channel.read(byteBuffer);
                byteBuffer.flip();
                if (byteBuffer.hasRemaining() && byteBuffer.get() == (byte) '\n') {
                    channel.truncate(position + 1);
                    channel.force(true);
                    return;
                }
                position--;
            }
            channel.truncate(0);
            channel.force(true);
        }
    }

    private void atomicWrite(Path file, Object value) {
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(json);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new HarnessStoreException("Cannot persist Harness snapshot " + file, e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The target snapshot has already been moved or the temp file is harmless.
                }
            }
        }
    }

    private <T> Optional<T> readOptional(Path file, Class<T> type) {
        return Files.exists(file) ? Optional.of(read(file, type)) : Optional.empty();
    }

    private <T> T readRequired(Path file, Class<T> type, String resource) {
        return readOptional(file, type)
            .orElseThrow(() -> new HarnessStoreException("Unknown " + resource + ": " + file));
    }

    private <T> T read(Path file, Class<T> type) {
        try {
            return objectMapper.readValue(file.toFile(), type);
        } catch (IOException e) {
            throw new HarnessStoreException("Cannot read Harness snapshot " + file, e);
        }
    }

    private Path ownerRoot(HarnessOwner owner) {
        requireSafeId("tenantId", owner.tenantId());
        return dataRoot.resolve(owner.tenantId()).resolve(Long.toString(owner.userId()));
    }

    private Path sessionDirectory(HarnessOwner owner, String sessionId) {
        requireSafeId("sessionId", sessionId);
        return ownerRoot(owner).resolve(sessionId);
    }

    private Path sessionFile(HarnessOwner owner, String sessionId) {
        return sessionDirectory(owner, sessionId).resolve("session.json");
    }

    private Path messagesFile(HarnessOwner owner, String sessionId) {
        return sessionDirectory(owner, sessionId).resolve("messages.jsonl");
    }

    private Path runDirectory(HarnessOwner owner, String sessionId, String runId) {
        requireSafeId("runId", runId);
        return sessionDirectory(owner, sessionId).resolve("runs").resolve(runId);
    }

    private Path runFile(HarnessOwner owner, String sessionId, String runId) {
        return runDirectory(owner, sessionId, runId).resolve("state.json");
    }

    private Path eventsFile(HarnessOwner owner, String sessionId, String runId) {
        return runDirectory(owner, sessionId, runId).resolve("events.jsonl");
    }

    private String sessionKey(HarnessOwner owner, String sessionId) {
        return owner.tenantId() + "/" + owner.userId() + "/" + sessionId;
    }

    private <T> T withSessionLock(HarnessOwner owner, String sessionId, Supplier<T> operation) {
        String key = sessionKey(owner, sessionId);
        ReentrantLock lock = sessionLocks.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    private void verifyOwner(HarnessOwner expected, HarnessOwner actual) {
        if (!expected.equals(actual)) {
            throw new HarnessStoreException("Harness resource does not belong to the authenticated owner");
        }
    }

    private void requireSafeId(String label, String value) {
        if (value == null || !value.matches(SAFE_ID)) {
            throw new IllegalArgumentException(label + " contains unsafe characters");
        }
    }

    private void validateRead(long afterSequence, int limit) {
        if (afterSequence < 0 || limit < 1 || limit > MAX_READ_LIMIT) {
            throw new IllegalArgumentException("Invalid ledger cursor or limit");
        }
    }
}
