package org.ruoyi.service.coding.harness.artifact;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * Owner/session/run-scoped, content-addressed artifact storage.
 *
 * <p>Writes are streamed into a same-filesystem staging file, size checked, SHA-256 hashed and
 * forced to durable storage before an atomic rename. A preview or {@link ArtifactRef} is created
 * only by reading the committed object. Stored objects are hash checked before every ranged read.
 * This class deliberately exposes no absolute filesystem paths.</p>
 */
public final class ArtifactStore {

    private static final int COPY_BUFFER_BYTES = 16 * 1024;
    private static final int LOCK_STRIPES = 64;
    private static final int MAX_MANIFEST_SCAN = 100_000;
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern MEDIA_TOKEN =
        Pattern.compile("[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]{0,126}");

    private final Path root;
    private final long maxInputBytes;
    private final int previewBytes;
    private final int maxReadBytes;
    private final Clock clock;
    private final ReentrantLock[] commitLocks = new ReentrantLock[LOCK_STRIPES];

    public ArtifactStore(Path root, long maxInputBytes, int previewBytes, int maxReadBytes) {
        this(root, maxInputBytes, previewBytes, maxReadBytes, Clock.systemUTC());
    }

    public ArtifactStore(Path root, long maxInputBytes, int previewBytes, int maxReadBytes,
                         Clock clock) {
        if (maxInputBytes < 0) {
            throw new IllegalArgumentException("maxInputBytes must not be negative");
        }
        if (previewBytes <= 0) {
            throw new IllegalArgumentException("previewBytes must be positive");
        }
        if (maxReadBytes <= 0) {
            throw new IllegalArgumentException("maxReadBytes must be positive");
        }
        this.clock = Objects.requireNonNull(clock, "clock");
        this.root = initializeRoot(root);
        this.maxInputBytes = maxInputBytes;
        this.previewBytes = previewBytes;
        this.maxReadBytes = maxReadBytes;
        for (int i = 0; i < commitLocks.length; i++) {
            commitLocks[i] = new ReentrantLock();
        }
    }

    public ArtifactRef put(ArtifactScope scope, String mediaType, byte[] content) {
        Objects.requireNonNull(content, "content");
        if (content.length > maxInputBytes) {
            throw new ArtifactLimitExceededException("Artifact exceeds the configured input limit");
        }
        return put(scope, mediaType, new ByteArrayInputStream(content));
    }

    /** The caller retains ownership of {@code content}; this method does not close it. */
    public ArtifactRef put(ArtifactScope scope, String mediaType, InputStream content) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(content, "content");
        String canonicalMediaType = requireMediaType(mediaType);
        Path temporary = null;
        try {
            Path staging = secureDirectory(List.of(".staging"), true, null);
            temporary = Files.createTempFile(staging, "pending-", ".artifact");
            validateRegularFile(temporary, null);
            WriteResult write = writeAndForce(temporary, content);

            ReentrantLock lock = commitLocks[Math.floorMod(
                Objects.hash(scope, write.hash()), commitLocks.length)];
            lock.lock();
            try {
                Path target = objectPath(scope, write.hash(), true);
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                    inspect(target, write.hash(), canonicalMediaType);
                    deleteTemporary(temporary);
                    temporary = null;
                } else {
                    moveAtomically(temporary, target);
                    temporary = null;
                }
                StoredObject stored = inspect(target, write.hash(), canonicalMediaType);
                if (stored.byteSize() != write.byteSize()) {
                    throw new ArtifactCorruptedException(write.hash(), stored.actualHash());
                }
                return new ArtifactRef(write.hash(), relativeHandle(scope, write.hash()),
                    canonicalMediaType, stored.byteSize(), stored.headPreview(),
                    stored.tailPreview(), stored.createdAt());
            } finally {
                lock.unlock();
            }
        } catch (ArtifactStoreException e) {
            throw e;
        } catch (IOException e) {
            throw new ArtifactWriteException("Artifact could not be durably committed", e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The write still fails and no handle/ref is exposed. A later janitor may clean staging.
                }
            }
        }
    }

    public byte[] read(ArtifactScope scope, String artifactId, long offset, int length) {
        Objects.requireNonNull(scope, "scope");
        String hash = requireHash(artifactId);
        Path target = objectPath(scope, hash, false);
        return verifiedSlice(target, hash, offset, length);
    }

    public byte[] read(ArtifactScope scope, ArtifactRef ref, long offset, int length) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(ref, "ref");
        String expectedHandle = relativeHandle(scope, ref.hash());
        if (!expectedHandle.equals(ref.relativeHandle())) {
            throw new ArtifactSecurityException("Artifact handle does not belong to the requested scope");
        }
        Path target = objectPath(scope, ref.hash(), false);
        BasicFileAttributes attributes = validateRegularFile(target, ref.hash());
        if (attributes.size() != ref.byteSize()) {
            throw new ArtifactCorruptedException(ref.hash(), "size-mismatch");
        }
        return verifiedSlice(target, ref.hash(), offset, length);
    }

    /** Reads and verifies a complete small object for provider multimodal input. */
    public byte[] readAll(ArtifactScope scope, String artifactId, int maximumBytes) {
        Objects.requireNonNull(scope, "scope");
        if (maximumBytes < 1 || maximumBytes > maxInputBytes) {
            throw new IllegalArgumentException("Invalid complete artifact read limit");
        }
        String hash = requireHash(artifactId);
        Path target = objectPath(scope, hash, false);
        BasicFileAttributes attributes = validateRegularFile(target, hash);
        if (attributes.size() > maximumBytes || attributes.size() > Integer.MAX_VALUE) {
            throw new ArtifactLimitExceededException("Artifact exceeds the complete read limit");
        }
        return scan(target, hash, 0, Math.toIntExact(attributes.size()), false).slice();
    }

    /** Performs a full stored-byte SHA-256 verification. */
    public boolean verify(ArtifactScope scope, String artifactId) {
        Objects.requireNonNull(scope, "scope");
        String hash = requireHash(artifactId);
        Path target = objectPath(scope, hash, false);
        ScanResult scan = scan(target, hash, -1, 0, false);
        return hash.equals(scan.actualHash());
    }

    /** Stable, owner/session-scoped discovery independent of the bounded model-context projection. */
    public ArtifactManifestPage listSession(ArtifactScope anchor, String afterCursor, int limit) {
        Objects.requireNonNull(anchor, "anchor");
        if (limit < 1 || limit > 256) {
            throw new IllegalArgumentException("Artifact manifest limit must be between 1 and 256");
        }
        String cursor = afterCursor == null || afterCursor.isBlank()
            ? null : requireManifestCursor(afterCursor);
        Path runs;
        try {
            runs = secureDirectory(List.of("owners", anchor.ownerId(), "sessions",
                anchor.sessionId(), "runs"), false, null);
        } catch (ArtifactNotFoundException missing) {
            return new ArtifactManifestPage(List.of(), null, false);
        }

        List<String> keys = new ArrayList<>();
        for (String runId : secureChildNames(runs, true)) {
            ArtifactScope scope = new ArtifactScope(anchor.ownerId(), anchor.sessionId(), runId);
            Path shaRoot;
            try {
                shaRoot = secureDirectory(List.of("owners", scope.ownerId(), "sessions",
                    scope.sessionId(), "runs", scope.runId(), "artifacts", "sha256"),
                    false, null);
            } catch (ArtifactNotFoundException missing) {
                continue;
            }
            for (String prefix : secureChildNames(shaRoot, true)) {
                if (!prefix.matches("[0-9a-f]{2}")) {
                    throw new ArtifactSecurityException("Artifact hash prefix is invalid");
                }
                Path prefixDirectory = secureDirectory(List.of("owners", scope.ownerId(),
                    "sessions", scope.sessionId(), "runs", scope.runId(), "artifacts",
                    "sha256", prefix), false, null);
                for (String hash : secureChildNames(prefixDirectory, false)) {
                    requireHash(hash);
                    if (!hash.startsWith(prefix)) {
                        throw new ArtifactSecurityException(
                            "Artifact hash does not match its prefix directory");
                    }
                    keys.add(scope.runId() + ":" + hash);
                    if (keys.size() > MAX_MANIFEST_SCAN) {
                        throw new ArtifactLimitExceededException(
                            "Artifact manifest exceeds the discovery scan limit");
                    }
                }
            }
        }
        keys.sort(String::compareTo);
        List<ArtifactManifestEntry> entries = new ArrayList<>();
        boolean hasMore = false;
        for (String key : keys) {
            if (cursor != null && key.compareTo(cursor) <= 0) {
                continue;
            }
            if (entries.size() == limit) {
                hasMore = true;
                break;
            }
            int separator = key.indexOf(':');
            String runId = key.substring(0, separator);
            String hash = key.substring(separator + 1);
            ArtifactScope scope = new ArtifactScope(anchor.ownerId(), anchor.sessionId(), runId);
            BasicFileAttributes attributes = validateRegularFile(objectPath(scope, hash, false), hash);
            long createdAt = attributes.creationTime().toMillis();
            if (createdAt <= 0) {
                createdAt = attributes.lastModifiedTime().toMillis();
            }
            entries.add(new ArtifactManifestEntry(runId, hash, attributes.size(),
                Math.max(1, createdAt)));
        }
        String next = entries.isEmpty() ? null : entries.get(entries.size() - 1).sourceRunId()
            + ":" + entries.get(entries.size() - 1).artifactId();
        return new ArtifactManifestPage(entries, next, hasMore);
    }

    private String requireManifestCursor(String value) {
        String cursor = value.strip().toLowerCase(Locale.ROOT);
        int separator = cursor.indexOf(':');
        if (separator <= 0 || separator != cursor.lastIndexOf(':')) {
            throw new IllegalArgumentException("Artifact manifest cursor is invalid");
        }
        String runId = cursor.substring(0, separator);
        String hash = cursor.substring(separator + 1);
        new ArtifactScope("owner", "session", runId);
        requireHash(hash);
        return runId + ":" + hash;
    }

    private List<String> secureChildNames(Path directory, boolean directories) {
        List<String> names = new ArrayList<>();
        try (var children = Files.list(directory)) {
            var iterator = children.iterator();
            while (iterator.hasNext()) {
                Path child = iterator.next();
                BasicFileAttributes attributes = Files.readAttributes(child,
                    BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                boolean expectedType = directories ? attributes.isDirectory()
                    : attributes.isRegularFile();
                if (!expectedType || attributes.isSymbolicLink() || attributes.isOther()
                    || !child.toRealPath().startsWith(root)) {
                    throw new ArtifactSecurityException(
                        "Artifact manifest contains a link or unexpected node");
                }
                names.add(child.getFileName().toString());
                if (names.size() > MAX_MANIFEST_SCAN) {
                    throw new ArtifactLimitExceededException(
                        "Artifact manifest directory exceeds the discovery scan limit");
                }
            }
        } catch (ArtifactStoreException error) {
            throw error;
        } catch (IOException error) {
            throw new ArtifactStoreException("Artifact manifest could not be listed", error);
        }
        names.sort(String::compareTo);
        return names;
    }

    private byte[] verifiedSlice(Path target, String hash, long offset, int length) {
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("Artifact offset and length must not be negative");
        }
        if (length > maxReadBytes) {
            throw new ArtifactLimitExceededException("Artifact read exceeds the configured limit");
        }
        BasicFileAttributes attributes = validateRegularFile(target, hash);
        if (offset > attributes.size() || (long) length > attributes.size() - offset) {
            throw new IllegalArgumentException("Artifact read range is outside the stored object");
        }
        return scan(target, hash, offset, length, false).slice();
    }

    private WriteResult writeAndForce(Path temporary, InputStream content) throws IOException {
        MessageDigest digest = sha256();
        long total = 0;
        byte[] buffer = new byte[COPY_BUFFER_BYTES];
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING, LinkOption.NOFOLLOW_LINKS)) {
            int read;
            while ((read = content.read(buffer)) != -1) {
                if (read == 0) {
                    continue;
                }
                if (total > maxInputBytes - read) {
                    throw new ArtifactLimitExceededException(
                        "Artifact exceeds the configured input limit");
                }
                digest.update(buffer, 0, read);
                ByteBuffer bytes = ByteBuffer.wrap(buffer, 0, read);
                while (bytes.hasRemaining()) {
                    channel.write(bytes);
                }
                total += read;
            }
            channel.force(true);
        }
        return new WriteResult(HexFormat.of().formatHex(digest.digest()), total);
    }

    private void moveAtomically(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            throw new ArtifactWriteException(
                "Artifact filesystem does not support atomic commit", e);
        } catch (FileAlreadyExistsException e) {
            // A different process won the identical content-addressed commit race.
            validateRegularFile(target, target.getFileName().toString());
            Files.delete(temporary);
        }
    }

    private StoredObject inspect(Path target, String expectedHash, String mediaType) {
        ScanResult scan = scan(target, expectedHash, -1, 0, true);
        BasicFileAttributes attributes = validateRegularFile(target, expectedHash);
        if (attributes.size() != scan.byteSize()) {
            throw new ArtifactCorruptedException(expectedHash, scan.actualHash());
        }
        long createdAt = attributes.creationTime().toMillis();
        if (createdAt <= 0) {
            createdAt = attributes.lastModifiedTime().toMillis();
        }
        if (createdAt <= 0) {
            createdAt = clock.millis();
        }
        return new StoredObject(scan.actualHash(), scan.byteSize(),
            encodePreview(scan.head(), mediaType), encodePreview(scan.tail(), mediaType), createdAt);
    }

    private ScanResult scan(Path target, String expectedHash, long sliceOffset, int sliceLength,
                            boolean capturePreviews) {
        BasicFileAttributes attributes = validateRegularFile(target, expectedHash);
        int capturedBytes = capturePreviews
            ? (int) Math.min((long) previewBytes, attributes.size()) : 0;
        byte[] head = new byte[capturedBytes];
        byte[] tail = new byte[capturedBytes];
        byte[] slice = sliceOffset >= 0 ? new byte[sliceLength] : new byte[0];
        int headCount = 0;
        int tailCount = 0;
        int sliceCount = 0;
        long position = 0;
        MessageDigest digest = sha256();
        byte[] bytes = new byte[COPY_BUFFER_BYTES];

        try (FileChannel channel = FileChannel.open(target, StandardOpenOption.READ,
            LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int read;
            while ((read = channel.read(buffer)) != -1) {
                if (read == 0) {
                    buffer.clear();
                    continue;
                }
                digest.update(bytes, 0, read);
                if (capturePreviews) {
                    int headCopy = Math.min(read, head.length - headCount);
                    if (headCopy > 0) {
                        System.arraycopy(bytes, 0, head, headCount, headCopy);
                        headCount += headCopy;
                    }
                    tailCount = appendTail(tail, tailCount, bytes, read);
                }
                if (sliceOffset >= 0 && sliceLength > 0) {
                    long chunkEnd = position + read;
                    long requestedEnd = sliceOffset + sliceLength;
                    long overlapStart = Math.max(position, sliceOffset);
                    long overlapEnd = Math.min(chunkEnd, requestedEnd);
                    if (overlapStart < overlapEnd) {
                        int source = (int) (overlapStart - position);
                        int count = (int) (overlapEnd - overlapStart);
                        System.arraycopy(bytes, source, slice, sliceCount, count);
                        sliceCount += count;
                    }
                }
                position += read;
                buffer.clear();
            }
        } catch (NoSuchFileException e) {
            throw new ArtifactNotFoundException(expectedHash);
        } catch (IOException e) {
            throw new ArtifactStoreException("Artifact could not be read", e);
        }

        String actualHash = HexFormat.of().formatHex(digest.digest());
        if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.US_ASCII),
            actualHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new ArtifactCorruptedException(expectedHash, actualHash);
        }
        if (position != attributes.size() || sliceCount != slice.length) {
            throw new ArtifactCorruptedException(expectedHash, actualHash);
        }
        return new ScanResult(actualHash, position, head, tail, slice);
    }

    private static int appendTail(byte[] tail, int tailCount, byte[] bytes, int length) {
        if (tail.length == 0) {
            return 0;
        }
        if (length >= tail.length) {
            System.arraycopy(bytes, length - tail.length, tail, 0, tail.length);
            return tail.length;
        }
        int overflow = Math.max(0, tailCount + length - tail.length);
        if (overflow > 0) {
            System.arraycopy(tail, overflow, tail, 0, tailCount - overflow);
            tailCount -= overflow;
        }
        System.arraycopy(bytes, 0, tail, tailCount, length);
        return tailCount + length;
    }

    private Path objectPath(ArtifactScope scope, String hash, boolean createDirectories) {
        List<String> directory = new ArrayList<>(scopeSegments(scope));
        directory.add("sha256");
        directory.add(hash.substring(0, 2));
        Path parent = secureDirectory(directory, createDirectories, hash);
        Path target = parent.resolve(hash).normalize();
        if (!target.startsWith(root)) {
            throw new ArtifactSecurityException("Artifact object path escapes the configured root");
        }
        if (!createDirectories && !Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new ArtifactNotFoundException(hash);
        }
        return target;
    }

    private Path secureDirectory(List<String> segments, boolean create, String missingArtifactId) {
        Path current = root;
        for (String segment : segments) {
            Path next = current.resolve(segment).normalize();
            if (!next.startsWith(root)) {
                throw new ArtifactSecurityException("Artifact directory escapes the configured root");
            }
            if (create) {
                try {
                    Files.createDirectory(next);
                } catch (FileAlreadyExistsException ignored) {
                    // Validated below without following the existing node.
                } catch (IOException e) {
                    throw new ArtifactWriteException("Artifact directory could not be created", e);
                }
            } else if (!Files.exists(next, LinkOption.NOFOLLOW_LINKS)) {
                throw new ArtifactNotFoundException(missingArtifactId);
            }
            BasicFileAttributes attributes;
            try {
                attributes = Files.readAttributes(next, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
                if (!attributes.isDirectory() || attributes.isSymbolicLink() || attributes.isOther()) {
                    throw new ArtifactSecurityException(
                        "Artifact path contains a link, junction, or non-directory node");
                }
                Path real = next.toRealPath();
                if (!real.startsWith(root)) {
                    throw new ArtifactSecurityException(
                        "Artifact directory resolves outside the configured root");
                }
                current = real;
            } catch (NoSuchFileException e) {
                throw new ArtifactNotFoundException(missingArtifactId);
            } catch (IOException e) {
                throw new ArtifactSecurityException("Artifact directory could not be validated", e);
            }
        }
        return current;
    }

    private BasicFileAttributes validateRegularFile(Path file, String artifactId) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile() || attributes.isSymbolicLink() || attributes.isOther()) {
                throw new ArtifactSecurityException(
                    "Artifact object is a link, junction, or non-regular file");
            }
            Path real = file.toRealPath();
            if (!real.startsWith(root)) {
                throw new ArtifactSecurityException(
                    "Artifact object resolves outside the configured root");
            }
            return attributes;
        } catch (NoSuchFileException e) {
            throw new ArtifactNotFoundException(artifactId);
        } catch (IOException e) {
            throw new ArtifactSecurityException("Artifact object could not be validated", e);
        }
    }

    private static Path initializeRoot(Path configuredRoot) {
        Objects.requireNonNull(configuredRoot, "root");
        Path absolute = configuredRoot.toAbsolutePath().normalize();
        try {
            Files.createDirectories(absolute);
            BasicFileAttributes attributes = Files.readAttributes(absolute,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory() || attributes.isSymbolicLink() || attributes.isOther()) {
                throw new ArtifactSecurityException(
                    "Configured artifact root must be a real directory");
            }
            return absolute.toRealPath();
        } catch (ArtifactStoreException e) {
            throw e;
        } catch (IOException e) {
            throw new ArtifactStoreException("Configured artifact root could not be initialized", e);
        }
    }

    private static List<String> scopeSegments(ArtifactScope scope) {
        return List.of("owners", scope.ownerId(), "sessions", scope.sessionId(),
            "runs", scope.runId(), "artifacts");
    }

    private static String relativeHandle(ArtifactScope scope, String hash) {
        return String.join("/", scopeSegments(scope)) + "/sha256/" + hash.substring(0, 2)
            + "/" + hash;
    }

    private static String encodePreview(byte[] bytes, String mediaType) {
        String baseType = mediaType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        if (baseType.startsWith("text/") || baseType.endsWith("+json")
            || baseType.endsWith("+xml") || baseType.equals("application/json")
            || baseType.equals("application/xml") || baseType.equals("application/javascript")) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return bytes.length == 0 ? "" : "base64:" + Base64.getEncoder().encodeToString(bytes);
    }

    static String requireHash(String hash) {
        if (hash == null || !SHA_256.matcher(hash).matches()) {
            throw new IllegalArgumentException("Artifact id must be a lowercase SHA-256 digest");
        }
        return hash;
    }

    static String requireMediaType(String mediaType) {
        if (mediaType == null || mediaType.isBlank() || mediaType.length() > 255
            || mediaType.indexOf('\r') >= 0 || mediaType.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Artifact mediaType is invalid");
        }
        String normalized = mediaType.strip();
        String baseType = normalized.split(";", 2)[0].strip();
        String[] components = baseType.split("/", -1);
        if (components.length != 2 || !MEDIA_TOKEN.matcher(components[0]).matches()
            || !MEDIA_TOKEN.matcher(components[1]).matches()) {
            throw new IllegalArgumentException("Artifact mediaType is invalid");
        }
        for (int i = 0; i < normalized.length(); i++) {
            char value = normalized.charAt(i);
            if (value < 0x20 || value == 0x7f) {
                throw new IllegalArgumentException("Artifact mediaType contains control characters");
            }
        }
        return normalized;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static void deleteTemporary(Path temporary) throws IOException {
        Files.delete(temporary);
    }

    private record WriteResult(String hash, long byteSize) {
    }

    private record StoredObject(
        String actualHash,
        long byteSize,
        String headPreview,
        String tailPreview,
        long createdAt
    ) {
    }

    private record ScanResult(
        String actualHash,
        long byteSize,
        byte[] head,
        byte[] tail,
        byte[] slice
    ) {
    }
}
