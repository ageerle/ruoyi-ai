package org.ruoyi.service.coding.harness.tool.command;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Builds one argv-only, networkless Docker invocation with a single workspace bind mount. */
final class DockerSandboxCommandBuilder {

    static final String CONTAINER_WORKSPACE = "/workspace";
    static final String CONTAINER_PREFIX = "ruoyi-harness-";
    static final String MANAGED_LABEL_KEY = "org.ruoyi.harness.managed";
    static final String MANAGED_LABEL_VALUE = "v1";
    static final String MAVEN_CACHE_ENV = "MAVEN_ARGS=--offline -Dmaven.repo.local="
        + CONTAINER_WORKSPACE + "/.harness-deps/maven";
    static final String PNPM_STORE_ENV = "npm_config_store_dir="
        + CONTAINER_WORKSPACE + "/.harness-deps/pnpm";
    static final String NPM_CACHE_ENV = "npm_config_cache="
        + CONTAINER_WORKSPACE + "/.harness-deps/npm";
    static final List<String> WORKSPACE_CACHE_ENVIRONMENT = List.of(
        MAVEN_CACHE_ENV, PNPM_STORE_ENV, NPM_CACHE_ENV);

    private final DockerSandboxConfig sandbox;
    private final Path leaseRoot;
    private final Supplier<String> nonceSupplier;

    DockerSandboxCommandBuilder(DockerSandboxConfig sandbox, Path leaseRoot) {
        this(sandbox, leaseRoot, () -> UUID.randomUUID().toString().replace("-", ""));
    }

    DockerSandboxCommandBuilder(DockerSandboxConfig sandbox, Path leaseRoot,
                                Supplier<String> nonceSupplier) {
        this.sandbox = sandbox;
        this.leaseRoot = leaseRoot.toAbsolutePath().normalize();
        this.nonceSupplier = nonceSupplier;
        sandbox.requireUsableDockerExecutable();
        validateMountSource(this.leaseRoot);
    }

    DockerSandboxInvocation build(String runId, Path workingDirectory, String executable,
                                  List<String> arguments) {
        return build(runId, workingDirectory, executable, arguments, false);
    }

    DockerSandboxInvocation build(String runId, Path workingDirectory, String executable,
                                  List<String> arguments, boolean workspaceReadOnly) {
        Path normalizedWorkingDirectory = workingDirectory.toAbsolutePath().normalize();
        if (!normalizedWorkingDirectory.startsWith(leaseRoot)) {
            throw new CommandToolException("CWD_OUTSIDE_WORKSPACE",
                "cwd escapes the immutable workspace lease");
        }
        validateMountSource(normalizedWorkingDirectory);
        String name = uniqueName(runId, nonceSupplier.get());
        String containerCwd = containerCwd(normalizedWorkingDirectory);
        String mount = "type=bind,source=" + leaseRoot + ",target=" + CONTAINER_WORKSPACE
            + ",bind-propagation=rprivate,bind-recursive=disabled"
            + (workspaceReadOnly ? ",readonly" : "");

        List<String> command = new ArrayList<>(48 + arguments.size());
        command.addAll(sandbox.dockerCommandPrefix());
        command.addAll(List.of(
            "create", "--pull", "never",
            "--name", name,
            "--label", MANAGED_LABEL_KEY + "=" + MANAGED_LABEL_VALUE,
            "--platform", "linux",
            "--read-only",
            "--network", "none",
            "--log-driver", "none",
            "--cap-drop", "ALL",
            "--security-opt", "no-new-privileges=true",
            "--security-opt", "seccomp=builtin",
            "--pids-limit", Integer.toString(sandbox.pidsLimit()),
            "--memory", Long.toString(sandbox.memoryBytes()),
            "--memory-swap", Long.toString(sandbox.memoryBytes()),
            "--cpus", sandbox.cpuLimit(),
            "--ipc", "none",
            "--cgroupns", "private",
            "--ulimit", "nofile=1024:1024",
            "--ulimit", "core=0",
            "--restart", "no",
            "--stop-timeout", "1",
            "--no-healthcheck",
            "--init",
            "--interactive",
            "--user", sandbox.user(),
            "--env", MAVEN_CACHE_ENV,
            "--env", PNPM_STORE_ENV,
            "--env", NPM_CACHE_ENV,
            "--tmpfs", "/tmp:rw,noexec,nosuid,nodev,size=" + sandbox.tmpfsBytes(),
            "--mount", mount,
            "--workdir", containerCwd,
            "--entrypoint", executable,
            sandbox.pinnedImage()
        ));
        command.addAll(arguments);
        return new DockerSandboxInvocation(name, command);
    }

    private String containerCwd(Path workingDirectory) {
        Path relative = leaseRoot.relativize(workingDirectory);
        if (relative.toString().isEmpty()) {
            return CONTAINER_WORKSPACE;
        }
        StringBuilder result = new StringBuilder(CONTAINER_WORKSPACE);
        for (Path segment : relative) {
            result.append('/').append(segment.toString().replace('\\', '/'));
        }
        return result.toString();
    }

    private static void validateMountSource(Path path) {
        try {
            DockerSandboxConfig.rejectControlOrComma(path.toString(), "Workspace path");
        } catch (IllegalArgumentException error) {
            throw new CommandToolException("UNSAFE_WORKSPACE_PATH",
                "Workspace and cwd paths must not contain commas or control characters", error);
        }
    }

    private static String uniqueName(String runId, String nonce) {
        String source = (runId == null ? "" : runId) + ':' + (nonce == null ? "" : nonce);
        String digest;
        try {
            digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
        return CONTAINER_PREFIX + digest.substring(0, 32);
    }
}
