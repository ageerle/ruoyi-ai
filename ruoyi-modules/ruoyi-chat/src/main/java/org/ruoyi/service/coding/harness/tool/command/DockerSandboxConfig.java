package org.ruoyi.service.coding.harness.tool.command;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable Docker/OCI isolation policy for one execute_process container. */
public record DockerSandboxConfig(
    String dockerExecutable,
    String dockerExecutableSha256,
    String dockerConfigDirectory,
    String dockerHost,
    String pinnedImage,
    int pidsLimit,
    long memoryBytes,
    int cpuMilliCores,
    long tmpfsBytes,
    String user
) {

    private static final Pattern PINNED_IMAGE = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Pattern USER = Pattern.compile("[1-9][0-9]{0,9}:[1-9][0-9]{0,9}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final String WINDOWS_LOCAL_HOST = "npipe:////./pipe/docker_engine";
    private static final String UNIX_LOCAL_HOST = "unix:///var/run/docker.sock";
    private static final Set<AclEntryPermission> MUTATING_ACL_PERMISSIONS = EnumSet.of(
        AclEntryPermission.WRITE_DATA,
        AclEntryPermission.APPEND_DATA,
        AclEntryPermission.WRITE_NAMED_ATTRS,
        AclEntryPermission.WRITE_ATTRIBUTES,
        AclEntryPermission.DELETE,
        AclEntryPermission.DELETE_CHILD,
        AclEntryPermission.WRITE_ACL,
        AclEntryPermission.WRITE_OWNER
    );

    public static final DockerSandboxConfig UNCONFIGURED = new DockerSandboxConfig(
        "", "", "", "", "", 256, 1024L * 1024 * 1024, 2_000,
        64L * 1024 * 1024, "65532:65532");

    public DockerSandboxConfig {
        dockerExecutable = dockerExecutable == null ? "" : dockerExecutable;
        dockerExecutableSha256 = dockerExecutableSha256 == null
            ? "" : dockerExecutableSha256.toLowerCase(java.util.Locale.ROOT);
        dockerConfigDirectory = dockerConfigDirectory == null ? "" : dockerConfigDirectory;
        dockerHost = dockerHost == null ? "" : dockerHost;
        pinnedImage = pinnedImage == null ? "" : pinnedImage;
        user = user == null ? "" : user;
        if (pidsLimit <= 0 || memoryBytes <= 0 || cpuMilliCores <= 0 || tmpfsBytes <= 0
            || !USER.matcher(user).matches()) {
            throw new IllegalArgumentException("Docker sandbox resource limits and non-root user are required");
        }
        if (!pinnedImage.isEmpty() && !PINNED_IMAGE.matcher(pinnedImage).matches()) {
            throw new IllegalArgumentException(
                "Docker sandbox image must be an evaluator-approved immutable local image id");
        }
        if (!dockerExecutableSha256.isEmpty()
            && !SHA256.matcher(dockerExecutableSha256).matches()) {
            throw new IllegalArgumentException(
                "Docker executable SHA-256 must be exactly 64 lowercase hex characters");
        }
        rejectControlOrComma(dockerExecutable, "Docker executable path");
        rejectControlOrComma(dockerExecutableSha256, "Docker executable SHA-256");
        rejectControlOrComma(dockerConfigDirectory, "Docker config directory");
        rejectControlOrComma(dockerHost, "Docker host endpoint");
        rejectControlOrComma(pinnedImage, "Docker image reference");
        if (!dockerHost.isEmpty() && !localDockerHost().equals(dockerHost)) {
            throw new IllegalArgumentException(
                "Docker sandbox host must be the platform-local engine endpoint");
        }
    }

    public static DockerSandboxConfig configured(String dockerExecutable,
                                                  String dockerExecutableSha256,
                                                  String dockerConfigDirectory,
                                                  String dockerHost,
                                                  String pinnedImage) {
        return new DockerSandboxConfig(dockerExecutable, dockerExecutableSha256,
            dockerConfigDirectory, dockerHost, pinnedImage, 256,
            1024L * 1024 * 1024, 2_000, 64L * 1024 * 1024, "65532:65532");
    }

    public static String localDockerHost() {
        return isWindows() ? WINDOWS_LOCAL_HOST : UNIX_LOCAL_HOST;
    }

    /** Fails before any process can start when the OS sandbox is not fully configured. */
    public Path requireUsableDockerExecutable() {
        if (dockerExecutable.isBlank() || dockerExecutableSha256.isBlank()
            || dockerConfigDirectory.isBlank()
            || dockerHost.isBlank() || pinnedImage.isBlank()) {
            throw new CommandToolException("SANDBOX_NOT_CONFIGURED",
                "execute_process requires an absolute Docker executable, empty config, "
                    + "local engine endpoint, and pinned image");
        }
        final Path path;
        try {
            path = Path.of(dockerExecutable);
        } catch (InvalidPathException error) {
            throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                "Configured Docker executable path is invalid", error);
        }
        Path normalized = path.toAbsolutePath().normalize();
        boolean windows = isWindows();
        if (!path.isAbsolute() || !Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)
            || Files.isSymbolicLink(normalized) || (!windows && !Files.isExecutable(normalized))) {
            throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                "Configured Docker executable is not an available absolute regular file");
        }
        try {
            if (!normalized.toRealPath().equals(normalized)) {
                throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                    "Configured Docker executable must not traverse links or reparse points");
            }
        } catch (java.io.IOException error) {
            throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                "Configured Docker executable cannot be resolved", error);
        }
        verifyTrustedFilesystemAccess(normalized);
        String fileName = normalized.getFileName() == null ? "" : normalized.getFileName().toString();
        if (!(fileName.equals("docker") || fileName.equalsIgnoreCase("docker.exe"))) {
            throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                "Configured sandbox runtime must be the Docker CLI executable");
        }
        Object identityBefore = requireStableIdentity(normalized, "Docker executable");
        verifyExecutableDigest(normalized);
        Object identityAfter = requireStableIdentity(normalized, "Docker executable");
        if (!identityBefore.equals(identityAfter)) {
            throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                "Configured Docker executable changed identity while it was verified");
        }
        requireUsableDockerConfigDirectory();
        if (!localDockerHost().equals(dockerHost)) {
            throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                "Configured Docker endpoint is not the platform-local engine");
        }
        return normalized;
    }

    private void verifyExecutableDigest(Path executable) {
        try (var input = Files.newInputStream(executable)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) {
                    digest.update(buffer, 0, count);
                }
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!MessageDigest.isEqual(actual.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    dockerExecutableSha256.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                    "Configured Docker executable did not match its approved SHA-256");
            }
        } catch (IOException error) {
            throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                "Configured Docker executable could not be hashed", error);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    /** Require an existing, absolute, link-free and empty CLI config directory. */
    public Path requireUsableDockerConfigDirectory() {
        final Path path;
        try {
            path = Path.of(dockerConfigDirectory);
        } catch (InvalidPathException error) {
            throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                "Configured Docker config directory is invalid", error);
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (!path.isAbsolute() || !Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)
            || Files.isSymbolicLink(normalized)) {
            throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                "Configured Docker config must be an absolute regular directory");
        }
        try {
            if (!normalized.toRealPath().equals(normalized)) {
                throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                    "Configured Docker config must not traverse links or reparse points");
            }
            verifyTrustedFilesystemAccess(normalized);
            Object identityBefore = requireStableIdentity(normalized, "Docker config directory");
            try (var entries = Files.list(normalized)) {
                if (entries.findAny().isPresent()) {
                    throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                        "Configured Docker config directory must remain empty");
                }
            }
            Object identityAfter = requireStableIdentity(normalized, "Docker config directory");
            if (!identityBefore.equals(identityAfter)) {
                throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                    "Configured Docker config changed identity while it was verified");
            }
        } catch (IOException error) {
            throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                "Configured Docker config directory cannot be verified", error);
        }
        return normalized;
    }

    private static Object requireStableIdentity(Path path, String field) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(path,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            Object fileKey = attributes.fileKey();
            if (fileKey != null) {
                return fileKey;
            }
            if (!isWindows()) {
                throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                    field + " filesystem does not expose a stable identity");
            }
            // Windows providers may omit fileKey even on NTFS. The path and its direct parent
            // have already passed owner/ACL checks, so an attribute snapshot plus the per-call
            // executable digest is the strongest portable fail-closed fallback available here.
            return new WindowsFileIdentity(attributes.creationTime().toMillis(),
                attributes.lastModifiedTime().toMillis(), attributes.size(),
                attributes.isDirectory(), attributes.isRegularFile());
        } catch (IOException error) {
            throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                field + " identity could not be verified", error);
        }
    }

    private static void verifyTrustedFilesystemAccess(Path target) {
        verifyTrustedFilesystemAccessOne(target);
        Path parent = target.getParent();
        if (parent != null) {
            verifyTrustedFilesystemAccessOne(parent);
        }
    }

    private static void verifyTrustedFilesystemAccessOne(Path path) {
        try {
            AclFileAttributeView aclView = Files.getFileAttributeView(path,
                AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            if (aclView != null) {
                String owner = aclView.getOwner().getName();
                if (!isTrustedWriter(owner)) {
                    throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                        "Docker runtime path owner is not evaluator-trusted");
                }
                for (var entry : aclView.getAcl()) {
                    if (entry.type() == AclEntryType.ALLOW
                        && !java.util.Collections.disjoint(entry.permissions(),
                            MUTATING_ACL_PERMISSIONS)
                        && !isTrustedWriter(entry.principal().getName())) {
                        throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                            "Docker runtime path is writable by an untrusted principal");
                    }
                }
                return;
            }

            PosixFileAttributeView posixView = Files.getFileAttributeView(path,
                PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            if (posixView == null) {
                throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                    "Docker runtime path permissions cannot be verified");
            }
            var attributes = posixView.readAttributes();
            if (!isTrustedWriter(attributes.owner().getName())) {
                throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                    "Docker runtime path owner is not evaluator-trusted");
            }
            Set<PosixFilePermission> permissions = attributes.permissions();
            if (permissions.contains(PosixFilePermission.GROUP_WRITE)
                || permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
                throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                    "Docker runtime path is group- or world-writable");
            }
        } catch (IOException error) {
            throw new CommandToolException("DOCKER_RUNTIME_UNAVAILABLE",
                "Docker runtime path permissions could not be verified", error);
        }
    }

    private static boolean isTrustedWriter(String principal) {
        String normalized = principal == null ? "" : principal.toLowerCase(Locale.ROOT);
        String currentUser = System.getProperty("user.name", "").toLowerCase(Locale.ROOT);
        return normalized.equals(currentUser)
            || (!currentUser.isBlank() && normalized.endsWith("\\" + currentUser))
            || normalized.equals("root")
            || normalized.endsWith("\\system")
            || normalized.endsWith("\\administrators")
            || normalized.endsWith("\\trustedinstaller");
    }

    private record WindowsFileIdentity(long createdMillis, long modifiedMillis, long size,
                                       boolean directory, boolean regularFile) {
    }

    List<String> dockerCommandPrefix() {
        return List.of(requireUsableDockerExecutable().toString(),
            "--config", requireUsableDockerConfigDirectory().toString(),
            "--host", dockerHost);
    }

    public String cpuLimit() {
        return String.format(java.util.Locale.ROOT, "%.3f", cpuMilliCores / 1000.0);
    }

    static void rejectControlOrComma(String value, String field) {
        if (value.indexOf(',') >= 0) {
            throw new IllegalArgumentException(field + " must not contain a comma");
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException(field + " must not contain control characters");
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT)
            .contains("win");
    }
}
