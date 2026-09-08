package org.ruoyi.service.coding.harness.tool.command;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/** The only host process boundary: an explicitly configured absolute Docker CLI executable. */
final class DockerCliRuntime implements DockerRuntime {

    private static final Pattern IMAGE_ID = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Pattern CONTAINER_ID = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern CONTAINER_NAME = Pattern.compile(
        Pattern.quote(DockerSandboxCommandBuilder.CONTAINER_PREFIX) + "[0-9a-f]{32}");
    private static final int CONTROL_OUTPUT_LIMIT = 8 * 1024;
    private static final long PREFLIGHT_TIMEOUT_MS = 5_000;
    private static final long CLEAN_ABSENCE_WINDOW_MS = 1_000;
    private static final long CLEAN_POLL_INTERVAL_MS = 100;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final Map<String, String> EXPECTED_IMAGE_ENVIRONMENT = Map.of(
        "PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
        "JAVA_HOME", "/opt/java/openjdk",
        "MAVEN_HOME", "/usr/share/maven",
        "HOME", "/tmp",
        "LANG", "C.UTF-8"
    );

    /** Shared across per-run runtime objects so one run's janitor never kills another active run. */
    private static final Map<String, String> ACTIVE_CONTAINER_IDS = new ConcurrentHashMap<>();
    private static final Set<String> UNCERTAIN_CREATE_NAMES = ConcurrentHashMap.newKeySet();
    private static final ReentrantLock CONTROL_PLANE_LOCK = new ReentrantLock(true);

    private final ProcessStarter processStarter;

    DockerCliRuntime() {
        this(DockerCliRuntime::startHostProcess);
    }

    DockerCliRuntime(ProcessStarter processStarter) {
        this.processStarter = Objects.requireNonNull(processStarter, "processStarter");
    }

    @Override
    public void verifyAvailable(DockerSandboxConfig sandbox) {
        Deadline deadline = Deadline.afterMillis(PREFLIGHT_TIMEOUT_MS);
        sandbox.requireUsableDockerExecutable();
        withControlPlaneLock(deadline, "DOCKER_RUNTIME_UNAVAILABLE", () -> {
            verifyDaemon(sandbox, deadline);
            verifyImage(sandbox, deadline);
            runManagedContainerJanitorLocked(sandbox, deadline);
            return null;
        });
    }

    @Override
    public Process start(DockerSandboxConfig sandbox, DockerSandboxInvocation invocation,
                         long timeoutMs) throws IOException {
        Deadline deadline = Deadline.afterMillis(timeoutMs);
        sandbox.requireUsableDockerExecutable();
        try {
            return withControlPlaneLock(deadline, "CONTAINER_START_TIMEOUT", () -> {
                List<String> createCommand = invocation.command();
                MountExpectation expectedMount = requireExactDockerCreateCommand(sandbox,
                    invocation.containerName(), createCommand);
                runManagedContainerJanitorLocked(sandbox, deadline);

                // Until Docker returns one immutable ID, a timed-out create may arrive late.
                UNCERTAIN_CREATE_NAMES.add(invocation.containerName());
                ControlResult created = control(createCommand, deadline,
                    "CONTAINER_CREATE_FAILED");
                String containerId = created.output().strip();
                if (created.exitCode() != 0 || !CONTAINER_ID.matcher(containerId).matches()) {
                    throw new CommandToolException("CONTAINER_CREATE_FAILED",
                        "Docker did not return one exact container id for the sandbox create phase");
                }
                ACTIVE_CONTAINER_IDS.put(invocation.containerName(), containerId);
                UNCERTAIN_CREATE_NAMES.remove(invocation.containerName());

                // create cannot execute repository code. Only after ID, labels, mounts and core
                // isolation facts are verified do we launch the attached start process.
                verifyCreatedContainer(sandbox, invocation.containerName(), containerId,
                    expectedMount, deadline);
                deadline.requireRemaining("CONTAINER_START_TIMEOUT");
                Process process = processStarter.start(command(sandbox, "start", "--attach",
                    "--interactive", "--detach-keys", "ctrl-],ctrl-]", containerId), false);
                if (deadline.expired()) {
                    process.destroyForcibly();
                    throw new CommandToolException("CONTAINER_START_TIMEOUT",
                        "Docker attached start exceeded the shared command wall deadline");
                }
                return process;
            });
        } catch (ProcessStartIOException error) {
            throw error.ioCause();
        }
    }

    @Override
    public void ensureRemoved(DockerSandboxConfig sandbox, String containerName, long timeoutMs) {
        Deadline deadline = Deadline.afterMillis(timeoutMs);
        boolean interrupted = Thread.interrupted();
        try {
            sandbox.requireUsableDockerExecutable();
            withControlPlaneLock(deadline, "CONTAINER_CLEANUP_FAILED", () -> {
                removeExactContainerAndProveAbsent(sandbox, containerName, deadline);
                return null;
            });
        } finally {
            interrupted |= Thread.interrupted();
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Deterministic test boundary; production invokes the same janitor at preflight and start. */
    void cleanManagedContainers(DockerSandboxConfig sandbox, long timeoutMs) {
        Deadline deadline = Deadline.afterMillis(timeoutMs);
        sandbox.requireUsableDockerExecutable();
        withControlPlaneLock(deadline, "CONTAINER_JANITOR_FAILED", () -> {
            runManagedContainerJanitorLocked(sandbox, deadline);
            return null;
        });
    }

    ProcessBuilder processBuilder(List<String> command) {
        ProcessBuilder builder = new ProcessBuilder(List.copyOf(command));
        builder.redirectErrorStream(false);
        builder.environment().clear();
        return builder;
    }

    private void verifyDaemon(DockerSandboxConfig sandbox, Deadline deadline) {
        ControlResult info = control(command(sandbox, "info", "--format",
            "{{.OSType}}|{{.Architecture}}|{{json .SecurityOptions}}"), deadline,
            "DOCKER_RUNTIME_UNAVAILABLE");
        String[] facts = info.output().strip().split("\\|", -1);
        if (info.exitCode() != 0 || facts.length != 3
            || !"linux".equals(facts[0]) || !isAmd64(facts[1])) {
            throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                "Docker daemon must be a Linux amd64 engine");
        }
        JsonNode securityOptions = parseJson(facts[2], "DOCKER_RUNTIME_UNTRUSTED",
            "Docker daemon security options are malformed");
        if (!securityOptions.isArray()) {
            throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                "Docker daemon security options are not an array");
        }
        boolean seccomp = false;
        boolean cgroupNamespace = false;
        for (JsonNode option : securityOptions) {
            String value = option.asText("");
            seccomp |= value.equals("name=seccomp") || value.startsWith("name=seccomp,");
            cgroupNamespace |= value.equals("name=cgroupns")
                || value.startsWith("name=cgroupns,");
        }
        if (!seccomp || !cgroupNamespace) {
            throw new CommandToolException("DOCKER_RUNTIME_UNTRUSTED",
                "Docker daemon must advertise seccomp and cgroup namespace isolation");
        }
    }

    private void verifyImage(DockerSandboxConfig sandbox, Deadline deadline) {
        // Docker's Go template treats an absent map key differently when it is combined with
        // other fields. Keep the guaranteed top-level identity fields in one bounded query and
        // parse Config as JSON so absent optional keys remain distinguishable from malformed
        // metadata without weakening any invariant.
        ControlResult image = control(command(sandbox, "image", "inspect", "--format",
            "{{.Id}}|{{.Os}}|{{.Architecture}}", sandbox.pinnedImage()),
            deadline, "SANDBOX_IMAGE_UNAVAILABLE");
        String[] facts = image.output().strip().split("\\|", -1);
        if (image.exitCode() != 0 || facts.length != 3 || !IMAGE_ID.matcher(facts[0]).matches()) {
            throw new CommandToolException("SANDBOX_IMAGE_UNAVAILABLE",
                "Pinned sandbox image is not available locally; pulling is forbidden");
        }
        if (!sandbox.pinnedImage().equals(facts[0])) {
            throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                "Configured local image id did not resolve to the exact inspected image");
        }
        if (!"linux".equals(facts[1]) || !isAmd64(facts[2])) {
            throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                "Pinned sandbox image must target Linux amd64 containers");
        }

        ControlResult configuration = control(command(sandbox, "image", "inspect", "--format",
            "{{json .Config}}", sandbox.pinnedImage()), deadline,
            "SANDBOX_IMAGE_UNAVAILABLE");
        if (configuration.exitCode() != 0) {
            throw new CommandToolException("SANDBOX_IMAGE_UNAVAILABLE",
                "Pinned sandbox image configuration could not be inspected");
        }
        JsonNode config = parseJson(configuration.output().strip(),
            "SANDBOX_IMAGE_UNTRUSTED", "Pinned sandbox image configuration is malformed");
        if (config == null || !config.isObject()) {
            throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                "Pinned sandbox image configuration must be one JSON object");
        }
        if (!sandbox.user().equals(text(config, "User"))
            || !"/workspace".equals(text(config, "WorkingDir"))) {
            throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                "Pinned sandbox image must declare the exact non-root user and workspace");
        }

        JsonNode volumes = config.get("Volumes");
        if (volumes != null && !volumes.isNull()
            && !(volumes.isObject() && volumes.size() == 0)) {
            throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                "Pinned sandbox image must not declare implicit volumes");
        }
        JsonNode command = config.get("Cmd");
        if (command != null && !command.isNull()
            && !(command.isArray() && command.size() == 0)) {
            throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                "Pinned sandbox image must not provide implicit command arguments");
        }
        JsonNode entrypoint = config.get("Entrypoint");
        if (entrypoint != null && !entrypoint.isNull()
            && !(entrypoint.isArray() && entrypoint.size() == 0)) {
            throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                "Pinned sandbox image must not provide an implicit entrypoint");
        }
        JsonNode healthcheck = config.get("Healthcheck");
        if (healthcheck != null && !healthcheck.isNull()) {
            throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                "Pinned sandbox image must not declare a healthcheck");
        }
        JsonNode labels = config.get("Labels");
        if (labels != null && !labels.isNull()
            && !(labels.isObject() && labels.size() == 0)) {
            throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                "Pinned sandbox image must not contribute implicit container labels");
        }

        JsonNode environment = config.get("Env");
        if (environment == null || !environment.isArray()) {
            throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                "Pinned sandbox image environment must be one exact JSON array");
        }
        Map<String, String> actual = new LinkedHashMap<>();
        for (JsonNode value : environment) {
            if (!value.isTextual()) {
                throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                    "Pinned sandbox image environment contains a non-text entry");
            }
            String entry = value.asText();
            int separator = entry.indexOf('=');
            if (separator <= 0 || actual.putIfAbsent(entry.substring(0, separator),
                entry.substring(separator + 1)) != null) {
                throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                    "Pinned sandbox image environment contains malformed or duplicate entries");
            }
        }
        if (!EXPECTED_IMAGE_ENVIRONMENT.equals(actual)) {
            throw new CommandToolException("SANDBOX_IMAGE_UNTRUSTED",
                "Pinned sandbox image environment differs from the exact evaluator allowlist");
        }
    }

    private MountExpectation requireExactDockerCreateCommand(DockerSandboxConfig sandbox,
                                                               String containerName,
                                                               List<String> command) {
        List<String> prefix = sandbox.dockerCommandPrefix();
        if (command == null || command.size() <= prefix.size()
            || !prefix.equals(command.subList(0, prefix.size()))
            || !"create".equals(command.get(prefix.size()))
            || !containerName.equals(singleOption(command, "--name"))
            || !(DockerSandboxCommandBuilder.MANAGED_LABEL_KEY + "="
                + DockerSandboxCommandBuilder.MANAGED_LABEL_VALUE).equals(
                    singleOption(command, "--label"))) {
            throw new CommandToolException("HOST_EXECUTION_DENIED",
                "Only an evaluator-labelled Docker create invocation may cross the host boundary");
        }
        List<String> environment = optionValues(command, "--env");
        if (!DockerSandboxCommandBuilder.WORKSPACE_CACHE_ENVIRONMENT.equals(environment)) {
            throw new CommandToolException("HOST_EXECUTION_DENIED",
                "Docker create environment differs from the exact workspace cache allowlist");
        }
        String mount = singleOption(command, "--mount");
        Map<String, String> fields = new LinkedHashMap<>();
        for (String token : mount.split(",", -1)) {
            int separator = token.indexOf('=');
            String key = separator < 0 ? token : token.substring(0, separator);
            String value = separator < 0 ? "" : token.substring(separator + 1);
            if (key.isBlank() || fields.putIfAbsent(key, value) != null) {
                throw new CommandToolException("HOST_EXECUTION_DENIED",
                    "Docker workspace mount contains malformed or duplicate fields");
            }
        }
        Set<String> expectedKeys = new HashSet<>(Set.of("type", "source", "target",
            "bind-propagation", "bind-recursive"));
        boolean readOnly = fields.containsKey("readonly");
        if (readOnly) {
            expectedKeys.add("readonly");
        }
        if (!expectedKeys.equals(fields.keySet()) || !"bind".equals(fields.get("type"))
            || fields.get("source").isBlank()
            || !DockerSandboxCommandBuilder.CONTAINER_WORKSPACE.equals(fields.get("target"))
            || !"rprivate".equals(fields.get("bind-propagation"))
            || !"disabled".equals(fields.get("bind-recursive"))
            || readOnly && !fields.get("readonly").isEmpty()) {
            throw new CommandToolException("HOST_EXECUTION_DENIED",
                "Docker workspace mount does not match the recursive-safe evaluator policy");
        }
        return new MountExpectation(fields.get("source"), readOnly);
    }

    private void verifyCreatedContainer(DockerSandboxConfig sandbox, String containerName,
                                        String containerId, MountExpectation expectedMount,
                                        Deadline deadline) {
        ControlResult inspect = control(command(sandbox, "container", "inspect", "--format",
            "{{.Id}}|{{.Name}}|{{.Image}}|{{.HostConfig.NetworkMode}}|"
                + "{{.HostConfig.ReadonlyRootfs}}|{{.Config.User}}|"
                + "{{.HostConfig.Privileged}}|{{.HostConfig.PidMode}}|{{.HostConfig.IpcMode}}|"
                + "{{json .Config.Labels}}", containerId), deadline,
            "CONTAINER_CREATE_UNTRUSTED");
        String[] facts = inspect.output().strip().split("\\|", -1);
        if (inspect.exitCode() != 0 || facts.length != 10
            || !containerId.equals(facts[0])
            || !("/" + containerName).equals(facts[1])
            || !sandbox.pinnedImage().equals(facts[2])
            || !"none".equals(facts[3])
            || !"true".equals(facts[4])
            || !sandbox.user().equals(facts[5])
            || !"false".equals(facts[6])
            || !facts[7].isEmpty()
            || !"none".equals(facts[8])) {
            throw new CommandToolException("CONTAINER_CREATE_UNTRUSTED",
                "Created container did not preserve the approved isolation facts");
        }
        JsonNode labels = parseJson(facts[9], "CONTAINER_CREATE_UNTRUSTED",
            "Created container labels are malformed");
        if (labels == null || !labels.isObject() || labels.size() != 1
            || !DockerSandboxCommandBuilder.MANAGED_LABEL_VALUE.equals(
                text(labels, DockerSandboxCommandBuilder.MANAGED_LABEL_KEY))) {
            throw new CommandToolException("CONTAINER_CREATE_UNTRUSTED",
                "Created container did not preserve the unique evaluator label");
        }

        ControlResult runtimeMounts = control(command(sandbox, "container", "inspect", "--format",
            "{{json .Mounts}}", containerId), deadline, "CONTAINER_CREATE_UNTRUSTED");
        JsonNode runtimeMount = onlyArrayElement(runtimeMounts, "CONTAINER_CREATE_UNTRUSTED",
            "Created container must expose exactly one runtime mount");
        JsonNode modeNode = runtimeMount.get("Mode");
        boolean textualMode = modeNode != null && modeNode.isTextual();
        String mode = textualMode ? modeNode.asText() : "";
        boolean exactMode = textualMode && (expectedMount.readOnly()
            ? mode.isEmpty() || "ro".equals(mode) || "ro,rprivate".equals(mode)
                || "rprivate,ro".equals(mode)
            : mode.isEmpty() || "rprivate".equals(mode) || "rw,rprivate".equals(mode));
        if (!"bind".equals(text(runtimeMount, "Type"))
            || !expectedMount.source().equals(text(runtimeMount, "Source"))
            || !DockerSandboxCommandBuilder.CONTAINER_WORKSPACE.equals(
                text(runtimeMount, "Destination"))
            || !exactBoolean(runtimeMount, "RW", !expectedMount.readOnly())
            || !"rprivate".equals(text(runtimeMount, "Propagation"))
            || !exactMode) {
            throw new CommandToolException("CONTAINER_CREATE_UNTRUSTED",
                "Created container runtime mount differs from the exact workspace bind policy");
        }

        ControlResult configuredMounts = control(command(sandbox, "container", "inspect",
            "--format", "{{json .HostConfig.Mounts}}", containerId), deadline,
            "CONTAINER_CREATE_UNTRUSTED");
        JsonNode configuredMount = onlyArrayElement(configuredMounts,
            "CONTAINER_CREATE_UNTRUSTED",
            "Created container must configure exactly one workspace mount");
        JsonNode bindOptions = configuredMount.path("BindOptions");
        JsonNode configuredReadOnly = configuredMount.get("ReadOnly");
        boolean exactConfiguredReadOnly = expectedMount.readOnly()
            ? configuredReadOnly != null && configuredReadOnly.isBoolean()
                && configuredReadOnly.asBoolean()
            : configuredReadOnly == null || configuredReadOnly.isBoolean()
                && !configuredReadOnly.asBoolean();
        if (!"bind".equals(text(configuredMount, "Type"))
            || !expectedMount.source().equals(text(configuredMount, "Source"))
            || !DockerSandboxCommandBuilder.CONTAINER_WORKSPACE.equals(
                text(configuredMount, "Target"))
            || !exactConfiguredReadOnly
            || !exactBoolean(bindOptions, "NonRecursive", true)
            || !"rprivate".equals(text(bindOptions, "Propagation"))) {
            throw new CommandToolException("CONTAINER_CREATE_UNTRUSTED",
                "Created container bind recursion or read-only policy was not preserved");
        }
    }

    private void runManagedContainerJanitorLocked(DockerSandboxConfig sandbox,
                                                   Deadline deadline) {
        List<ManagedContainer> containers = listManagedContainers(sandbox, deadline,
            "CONTAINER_JANITOR_FAILED");
        Set<String> activeIds = Set.copyOf(ACTIVE_CONTAINER_IDS.values());
        for (ManagedContainer container : containers) {
            if (activeIds.contains(container.id())) {
                continue;
            }
            ControlResult removal = control(command(sandbox, "rm", "-f", container.id()),
                deadline, "CONTAINER_JANITOR_FAILED");
            if (removal.exitCode() != 0) {
                throw new CommandToolException("CONTAINER_JANITOR_FAILED",
                    "Docker could not remove an evaluator-labelled stale container");
            }
        }
        for (ManagedContainer remaining : listManagedContainers(sandbox, deadline,
            "CONTAINER_JANITOR_FAILED")) {
            if (!activeIds.contains(remaining.id())) {
                throw new CommandToolException("CONTAINER_JANITOR_FAILED",
                    "Docker did not prove stale evaluator container removal");
            }
        }
    }

    private List<ManagedContainer> listManagedContainers(DockerSandboxConfig sandbox,
                                                          Deadline deadline,
                                                          String failureCode) {
        ControlResult listing = control(command(sandbox, "container", "ls", "--all",
            "--no-trunc", "--filter", "label=" + DockerSandboxCommandBuilder.MANAGED_LABEL_KEY
                + "=" + DockerSandboxCommandBuilder.MANAGED_LABEL_VALUE,
            "--filter", "name=^/" + DockerSandboxCommandBuilder.CONTAINER_PREFIX,
            "--format", "{{.ID}}|{{.Names}}|{{.Labels}}"),
            deadline, failureCode);
        if (listing.exitCode() != 0) {
            throw new CommandToolException(failureCode,
                "Docker could not enumerate evaluator-labelled containers");
        }
        return parseManagedContainers(listing.output(), failureCode);
    }

    private void removeExactContainerAndProveAbsent(DockerSandboxConfig sandbox,
                                                     String containerName,
                                                     Deadline deadline) {
        // Once execution enters cleanup this ID is no longer active. If the current cleanup
        // fails, the next startup/execution janitor must be allowed to recover it by label.
        String knownId = ACTIVE_CONTAINER_IDS.remove(containerName);
        if (knownId != null) {
            requireSuccessfulRemoval(sandbox, knownId, deadline);
        }
        long absentSince = -1;
        while (!deadline.expired()) {
            ControlResult listing = control(command(sandbox, "container", "ls", "--all",
                "--no-trunc", "--filter", "name=^/" + containerName + "$",
                "--format", "{{.ID}}|{{.Names}}|{{.Labels}}"),
                deadline, "CONTAINER_CLEANUP_FAILED");
            if (listing.exitCode() != 0) {
                throw new CommandToolException("CONTAINER_CLEANUP_FAILED",
                    "Docker daemon could not prove sandbox container absence");
            }
            List<ManagedContainer> matches = parseManagedContainers(listing.output(),
                "CONTAINER_CLEANUP_FAILED");
            if (matches.isEmpty()) {
                if (absentSince < 0) {
                    absentSince = System.nanoTime();
                }
                if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - absentSince)
                    >= CLEAN_ABSENCE_WINDOW_MS) {
                    if (UNCERTAIN_CREATE_NAMES.contains(containerName)) {
                        throw new CommandToolException("CONTAINER_CLEANUP_UNCERTAIN",
                            "A timed-out Docker create may still arrive late; a later labelled "
                                + "janitor pass is required");
                    }
                    return;
                }
            } else {
                absentSince = -1;
                if (matches.size() != 1 || !containerName.equals(matches.get(0).name())) {
                    throw new CommandToolException("CONTAINER_CLEANUP_FAILED",
                        "Exact-name cleanup observed an unexpected managed container identity");
                }
                requireSuccessfulRemoval(sandbox, matches.get(0).id(), deadline);
            }
            sleepWithin(deadline, CLEAN_POLL_INTERVAL_MS, "CONTAINER_CLEANUP_FAILED");
        }
        throw new CommandToolException("CONTAINER_CLEANUP_FAILED",
            "Docker daemon did not prove stable sandbox container absence within the wall deadline");
    }

    private void requireSuccessfulRemoval(DockerSandboxConfig sandbox, String containerId,
                                          Deadline deadline) {
        ControlResult removal = control(command(sandbox, "rm", "-f", containerId), deadline,
            "CONTAINER_CLEANUP_FAILED");
        if (removal.exitCode() != 0) {
            throw new CommandToolException("CONTAINER_CLEANUP_FAILED",
                "Docker could not remove the exact evaluator container id");
        }
    }

    private List<ManagedContainer> parseManagedContainers(String output, String failureCode) {
        if (output.isBlank()) {
            return List.of();
        }
        List<ManagedContainer> containers = new ArrayList<>();
        for (String line : output.lines().filter(value -> !value.isBlank()).toList()) {
            String[] facts = line.strip().split("\\|", -1);
            if (facts.length != 3 || !CONTAINER_ID.matcher(facts[0]).matches()
                || !CONTAINER_NAME.matcher(facts[1]).matches()
                || !(DockerSandboxCommandBuilder.MANAGED_LABEL_KEY + "="
                    + DockerSandboxCommandBuilder.MANAGED_LABEL_VALUE).equals(facts[2])) {
                throw new CommandToolException(failureCode,
                    "Docker returned a malformed evaluator-managed container identity");
            }
            containers.add(new ManagedContainer(facts[0], facts[1]));
        }
        return List.copyOf(containers);
    }

    private JsonNode onlyArrayElement(ControlResult result, String failureCode, String message) {
        if (result.exitCode() != 0) {
            throw new CommandToolException(failureCode, message);
        }
        JsonNode array = parseJson(result.output().strip(), failureCode, message);
        if (array == null || !array.isArray() || array.size() != 1) {
            throw new CommandToolException(failureCode, message);
        }
        return array.get(0);
    }

    private static JsonNode parseJson(String value, String failureCode, String message) {
        try {
            return OBJECT_MAPPER.readTree(value);
        } catch (IOException error) {
            throw new CommandToolException(failureCode, message, error);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : "";
    }

    private static boolean exactBoolean(JsonNode node, String field, boolean expected) {
        JsonNode value = node.path(field);
        return value.isBoolean() && value.asBoolean() == expected;
    }

    private static boolean isAmd64(String architecture) {
        return "amd64".equals(architecture) || "x86_64".equals(architecture);
    }

    private static String singleOption(List<String> command, String option) {
        String value = null;
        for (int index = 0; index + 1 < command.size(); index++) {
            if (option.equals(command.get(index))) {
                if (value != null) {
                    throw new CommandToolException("HOST_EXECUTION_DENIED",
                        "Docker create contains a duplicate " + option + " option");
                }
                value = command.get(index + 1);
            }
        }
        if (value == null) {
            throw new CommandToolException("HOST_EXECUTION_DENIED",
                "Docker create is missing the required " + option + " option");
        }
        return value;
    }

    private static List<String> optionValues(List<String> command, String option) {
        List<String> values = new ArrayList<>();
        for (int index = 0; index < command.size(); index++) {
            if (!option.equals(command.get(index))) {
                continue;
            }
            if (index + 1 >= command.size() || command.get(index + 1).startsWith("--")) {
                throw new CommandToolException("HOST_EXECUTION_DENIED",
                    "Docker create contains a malformed " + option + " option");
            }
            values.add(command.get(index + 1));
        }
        return List.copyOf(values);
    }

    private List<String> command(DockerSandboxConfig sandbox, String... arguments) {
        List<String> command = new ArrayList<>(sandbox.dockerCommandPrefix());
        command.addAll(Arrays.asList(arguments));
        return List.copyOf(command);
    }

    private ControlResult control(List<String> command, Deadline deadline, String failureCode) {
        Process process = null;
        BoundedOutputCollector output = null;
        Thread collector = null;
        try {
            deadline.requireRemaining(failureCode);
            process = processStarter.start(command, true);
            output = new BoundedOutputCollector(process.getInputStream(), CONTROL_OUTPUT_LIMIT);
            collector = new Thread(output, "harness-docker-control-output");
            collector.setDaemon(true);
            collector.start();
            if (!process.waitFor(deadline.remainingMillis(failureCode), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                output.close();
                throw new CommandToolException(failureCode,
                    "Docker control command exceeded the shared wall deadline");
            }
            if (collector.isAlive()) {
                collector.join(deadline.remainingMillis(failureCode));
            }
            if (collector.isAlive()) {
                output.close();
                throw new CommandToolException(failureCode,
                    "Docker control command output exceeded the shared wall deadline");
            }
            if (output.failure() != null || output.truncated()) {
                throw new CommandToolException(failureCode,
                    "Docker control command output could not be captured exactly",
                    output.failure());
            }
            return new ControlResult(process.exitValue(), output.content());
        } catch (IOException error) {
            throw new CommandToolException(failureCode,
                "Docker control command could not be started", error);
        } catch (InterruptedException error) {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw new CommandToolException(failureCode,
                "Docker control command was interrupted", error);
        } finally {
            if (output != null && collector != null && collector.isAlive()) {
                output.close();
            }
        }
    }

    private static <T> T withControlPlaneLock(Deadline deadline, String failureCode,
                                               CheckedSupplier<T> operation) {
        boolean locked = false;
        try {
            locked = CONTROL_PLANE_LOCK.tryLock(deadline.remainingMillis(failureCode),
                TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new CommandToolException(failureCode,
                    "Docker control plane lock exceeded the shared wall deadline");
            }
            return operation.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new CommandToolException(failureCode,
                "Docker control plane lock was interrupted", error);
        } catch (IOException error) {
            throw new ProcessStartIOException(error);
        } finally {
            if (locked) {
                CONTROL_PLANE_LOCK.unlock();
            }
        }
    }

    private static void sleepWithin(Deadline deadline, long maximumMs, String failureCode) {
        try {
            TimeUnit.MILLISECONDS.sleep(Math.min(maximumMs,
                deadline.remainingMillis(failureCode)));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new CommandToolException(failureCode,
                "Docker cleanup observation was interrupted", error);
        }
    }

    private static Process startHostProcess(List<String> command, boolean mergeError)
        throws IOException {
        ProcessBuilder builder = new ProcessBuilder(List.copyOf(command));
        builder.redirectErrorStream(mergeError);
        builder.environment().clear();
        return builder.start();
    }

    @FunctionalInterface
    interface ProcessStarter {
        Process start(List<String> command, boolean mergeError) throws IOException;
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws IOException;
    }

    private static final class ProcessStartIOException extends RuntimeException {
        private ProcessStartIOException(IOException cause) {
            super(cause);
        }

        private IOException ioCause() {
            return (IOException) getCause();
        }
    }

    private record ControlResult(int exitCode, String output) {
    }

    private record MountExpectation(String source, boolean readOnly) {
    }

    private record ManagedContainer(String id, String name) {
    }

    private static final class Deadline {
        private final long deadlineNanos;

        private Deadline(long deadlineNanos) {
            this.deadlineNanos = deadlineNanos;
        }

        static Deadline afterMillis(long timeoutMs) {
            long duration = TimeUnit.MILLISECONDS.toNanos(Math.max(1, timeoutMs));
            long now = System.nanoTime();
            long deadline = now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
            return new Deadline(deadline);
        }

        boolean expired() {
            return deadlineNanos - System.nanoTime() <= 0;
        }

        long remainingMillis(String failureCode) {
            long remaining = deadlineNanos - System.nanoTime();
            if (remaining <= 0) {
                throw new CommandToolException(failureCode,
                    "Docker operation exhausted the shared wall deadline");
            }
            return Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining));
        }

        void requireRemaining(String failureCode) {
            remainingMillis(failureCode);
        }
    }
}
