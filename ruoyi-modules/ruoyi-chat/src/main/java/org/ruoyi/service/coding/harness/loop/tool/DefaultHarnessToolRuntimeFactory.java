package org.ruoyi.service.coding.harness.loop.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;
import org.ruoyi.service.coding.harness.model.HarnessVerificationMode;
import org.ruoyi.service.coding.harness.modelruntime.HarnessAnalysisDelegate;
import org.ruoyi.service.coding.harness.plan.ExecutionMode;
import org.ruoyi.service.coding.harness.plan.tool.HarnessPlanCommandService;
import org.ruoyi.service.coding.harness.plan.tool.HarnessPlanToolDescriptors;
import org.ruoyi.service.coding.harness.artifact.HarnessArtifactRepository;
import org.ruoyi.service.coding.harness.artifact.HarnessArtifactToolDescriptors;
import org.ruoyi.service.coding.harness.artifact.HarnessArtifactTools;
import org.ruoyi.service.coding.harness.skill.HarnessSkillCatalog;
import org.ruoyi.service.coding.harness.skill.HarnessSkillCatalogFactory;
import org.ruoyi.service.coding.harness.skill.HarnessSkillTools;
import org.ruoyi.service.coding.harness.tool.ToolCapability;
import org.ruoyi.service.coding.harness.tool.ToolDescriptor;
import org.ruoyi.service.coding.harness.tool.builtin.BuiltinCodingTools;
import org.ruoyi.service.coding.harness.tool.builtin.BuiltinToolDescriptors;
import org.ruoyi.service.coding.harness.tool.builtin.BuiltinToolLimits;
import org.ruoyi.service.coding.harness.tool.builtin.RunContext;
import org.ruoyi.service.coding.harness.tool.command.CommandToolConfig;
import org.ruoyi.service.coding.harness.tool.command.CommandToolDescriptors;
import org.ruoyi.service.coding.harness.tool.command.DockerSandboxConfig;
import org.ruoyi.service.coding.harness.tool.command.ExecuteProcessTool;
import org.ruoyi.service.coding.harness.tool.command.InlineProbeTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class DefaultHarnessToolRuntimeFactory implements HarnessToolRuntimeFactory {

    private static final List<ToolDescriptor> SKILL_DESCRIPTORS = List.of(
        new ToolDescriptor("activate_skill", Set.of(ToolCapability.READ, ToolCapability.CONTROL),
            true, 5_000, 16 * 1024, 256 * 1024, true,
            "Loads one exact skill body into the current model context"),
        new ToolDescriptor("read_skill_resource", Set.of(ToolCapability.READ), true,
            5_000, 32 * 1024, 1024 * 1024, true,
            "Reads one bounded resource belonging to an available skill")
    );

    private final ObjectMapper objectMapper;
    private final HarnessSkillCatalogFactory skillCatalogFactory;
    private final HarnessPlanCommandService planCommandService;
    private final HarnessArtifactRepository artifactRepository;
    private final boolean executeProcessEnabled;
    private final CommandToolConfig commandToolConfig;
    private final HarnessAnalysisDelegate analysisDelegate;

    private static final ToolDescriptor DELEGATE_DESCRIPTOR = new ToolDescriptor(
        "delegate_task", Set.of(ToolCapability.READ), true, 120_000,
        96 * 1024, 32 * 1024, true,
        "Runs one bounded advisory model worker with no repository or mutation tools");

    @Autowired
    public DefaultHarnessToolRuntimeFactory(ObjectMapper objectMapper,
                                            HarnessSkillCatalogFactory skillCatalogFactory,
                                            HarnessPlanCommandService planCommandService,
                                            HarnessArtifactRepository artifactRepository,
                                            HarnessAnalysisDelegate analysisDelegate,
                                            @Value("${coding.harness.tools.execute-process.enabled:false}")
                                            boolean executeProcessEnabled,
                                            @Value("${coding.harness.tools.execute-process.sandbox.docker-executable:}")
                                            String dockerExecutable,
                                            @Value("${coding.harness.tools.execute-process.sandbox.docker-executable-sha256:}")
                                            String dockerExecutableSha256,
                                            @Value("${coding.harness.tools.execute-process.sandbox.docker-config-directory:}")
                                            String dockerConfigDirectory,
                                            @Value("${coding.harness.tools.execute-process.sandbox.docker-host:}")
                                            String dockerHost,
                                            @Value("${coding.harness.tools.execute-process.sandbox.image:}")
                                            String pinnedImage) {
        this(objectMapper, skillCatalogFactory, planCommandService, artifactRepository,
            analysisDelegate, executeProcessEnabled,
            CommandToolConfig.DEFAULT.withDockerSandbox(
                DockerSandboxConfig.configured(dockerExecutable, dockerExecutableSha256,
                    dockerConfigDirectory, dockerHost, pinnedImage)));
    }

    private DefaultHarnessToolRuntimeFactory(ObjectMapper objectMapper,
                                             HarnessSkillCatalogFactory skillCatalogFactory,
                                             HarnessPlanCommandService planCommandService,
                                             HarnessArtifactRepository artifactRepository,
                                             HarnessAnalysisDelegate analysisDelegate,
                                             boolean executeProcessEnabled,
                                             CommandToolConfig commandToolConfig) {
        this.objectMapper = objectMapper;
        this.skillCatalogFactory = skillCatalogFactory;
        this.planCommandService = planCommandService;
        this.artifactRepository = artifactRepository;
        this.analysisDelegate = analysisDelegate;
        this.executeProcessEnabled = executeProcessEnabled;
        this.commandToolConfig = Objects.requireNonNull(commandToolConfig, "commandToolConfig");
        if (executeProcessEnabled) {
            this.commandToolConfig.dockerSandbox().requireUsableDockerExecutable();
        }
    }

    /** Programmatic construction is fail-closed unless command execution is explicitly enabled. */
    public DefaultHarnessToolRuntimeFactory(ObjectMapper objectMapper,
                                            HarnessSkillCatalogFactory skillCatalogFactory,
                                            HarnessPlanCommandService planCommandService,
                                            HarnessArtifactRepository artifactRepository,
                                            boolean executeProcessEnabled) {
        this(objectMapper, skillCatalogFactory, planCommandService, artifactRepository, null,
            executeProcessEnabled, CommandToolConfig.DEFAULT);
    }

    /** Explicit programmatic sandbox configuration; never substitutes a host execution path. */
    public DefaultHarnessToolRuntimeFactory(ObjectMapper objectMapper,
                                            HarnessSkillCatalogFactory skillCatalogFactory,
                                            HarnessPlanCommandService planCommandService,
                                            HarnessArtifactRepository artifactRepository,
                                            boolean executeProcessEnabled,
                                            CommandToolConfig commandToolConfig) {
        this(objectMapper, skillCatalogFactory, planCommandService, artifactRepository, null,
            executeProcessEnabled, commandToolConfig);
    }

    /** Programmatic construction is fail-closed unless command execution is explicitly enabled. */
    public DefaultHarnessToolRuntimeFactory(ObjectMapper objectMapper,
                                            HarnessSkillCatalogFactory skillCatalogFactory,
                                            HarnessPlanCommandService planCommandService,
                                            HarnessArtifactRepository artifactRepository) {
        this(objectMapper, skillCatalogFactory, planCommandService, artifactRepository, null, false,
            CommandToolConfig.DEFAULT);
    }

    @Override
    public HarnessToolRuntime create(HarnessSessionState session, HarnessRunState run) {
        // 外部验收模式：编程智能体不暴露 execute_process/run_inline_probe 等测试/进程工具，
        // 验证由独立外部验收者完成。默认 AGENT 模式保持原有安全、计划、审批、验证逻辑。
        boolean externalVerification = session != null
            && session.verificationMode() == HarnessVerificationMode.EXTERNAL;
        Path workspace = Path.of(session.workspace());
        RunContext context = new RunContext(run.runId(), workspace, BuiltinToolLimits.DEFAULT, true);
        BuiltinCodingTools codingTools = new BuiltinCodingTools(context);
        HarnessSkillCatalog skills = skillCatalogFactory.load(context.leaseRoot());
        HarnessSkillTools skillTools = new HarnessSkillTools(skills);
        var planTools = planCommandService.bind(run.owner(), run.sessionId(), run.runId());
        var artifactTools = new HarnessArtifactTools(artifactRepository, run.owner(),
            run.sessionId(), run.runId());
        HarnessToolRegistry.Builder registryBuilder = HarnessToolRegistry.builder(objectMapper)
            .registerAnnotatedSubset(codingTools, codingDescriptorsFor(run, context))
            .registerAnnotatedSubset(artifactTools, artifactDescriptorsFor(run));
        if (analysisDelegate != null && (run.executionPlan() == null
            || run.executionPlan().mode() == ExecutionMode.PLAN
            || run.executionPlan().mode() == ExecutionMode.VERIFY)) {
            registryBuilder.registerAnnotated(new HarnessDelegateTools(analysisDelegate, session, run),
                List.of(DELEGATE_DESCRIPTOR));
        }
        if (run.executionPlan() == null
            || run.executionPlan().mode() == ExecutionMode.PLAN) {
            // Skills help discovery and planning. Once a plan is approved, repository truth and
            // the durable contract are authoritative; retaining skill schemas on every coding
            // turn adds cost without adding implementation authority.
            registryBuilder.registerAnnotated(skillTools, SKILL_DESCRIPTORS);
        }
        List<ToolDescriptor> planDescriptors = planDescriptorsFor(run);
        if (!planDescriptors.isEmpty()) {
            registryBuilder.registerAnnotatedSubset(planTools, planDescriptors);
        }
        ExecutionMode mode = run.executionPlan() == null ? null : run.executionPlan().mode();
        if (!externalVerification && executeProcessEnabled
            && (mode == null || mode == ExecutionMode.BUILD || mode == ExecutionMode.VERIFY)) {
            ExecuteProcessTool commandTool = new ExecuteProcessTool(context,
                commandToolConfig);
            if (mode == ExecutionMode.BUILD) {
                registryBuilder.registerAnnotated(commandTool,
                    List.of(CommandToolDescriptors.executeProcess(commandTool.config())));
            }
            registryBuilder.registerAnnotated(new InlineProbeTool(commandTool,
                    mode != ExecutionMode.BUILD),
                List.of(CommandToolDescriptors.inlineProbe(commandTool.config())));
        }
        HarnessToolRegistry registry = registryBuilder.build();
        return new HarnessToolRuntime(registry, skills);
    }

    /**
     * Advertise the smallest coding surface that can make progress in the current phase. Besides
     * reducing provider cost, this is an authority boundary: VERIFY cannot mutate the workspace,
     * and PLAN cannot mutate it before approval. The complete registry is rebuilt on every phase
     * transition by the run processor, so BUILD still receives the full implementation surface.
     */
    static List<ToolDescriptor> codingDescriptorsFor(HarnessRunState run, RunContext context) {
        ExecutionMode mode = run.executionPlan() == null ? null : run.executionPlan().mode();
        return codingDescriptorsFor(mode, context);
    }

    static List<ToolDescriptor> codingDescriptorsFor(ExecutionMode mode, RunContext context) {
        Set<String> allowed = mode == ExecutionMode.BUILD
            ? Set.of("read_source", "search_text", "git_diff", "write_file", "replace_text")
            : mode == ExecutionMode.VERIFY
                ? Set.of("read_source", "search_text", "git_diff")
                : Set.of("read_file", "read_source", "list_files", "glob_files", "search_text",
                    "git_status", "git_diff");
        return BuiltinToolDescriptors.all(context).stream()
            .filter(descriptor -> allowed.contains(descriptor.toolName()))
            .filter(descriptor -> !Set.of("git_status", "git_diff")
                .contains(descriptor.toolName()) || leaseRootIsGitRepository(context.leaseRoot()))
            .toList();
    }

    /** Match the stricter execution precondition so unavailable Git tools are never advertised. */
    private static boolean leaseRootIsGitRepository(Path leaseRoot) {
        Path metadata = leaseRoot.resolve(".git");
        return Files.isDirectory(metadata, LinkOption.NOFOLLOW_LINKS)
            && Files.isRegularFile(metadata.resolve("HEAD"), LinkOption.NOFOLLOW_LINKS);
    }

    static List<ToolDescriptor> artifactDescriptorsFor(HarnessRunState run) {
        ExecutionMode mode = run.executionPlan() == null ? null : run.executionPlan().mode();
        Set<String> allowed = mode == ExecutionMode.BUILD || mode == ExecutionMode.VERIFY
            ? Set.of("read_artifact") : Set.of("read_artifact", "list_artifacts");
        return HarnessArtifactToolDescriptors.all().stream()
            .filter(descriptor -> allowed.contains(descriptor.toolName()))
            .toList();
    }

    /** Advertise only control transitions legal for the current durable phase. */
    static List<ToolDescriptor> planDescriptorsFor(HarnessRunState run) {
        ExecutionMode mode = run.executionPlan() == null ? null : run.executionPlan().mode();
        Set<String> allowed = mode == null || mode == ExecutionMode.PLAN
            ? Set.of("plan_create")
            : mode == ExecutionMode.BUILD
                ? Set.of("plan_step", "plan_verify")
                : mode == ExecutionMode.VERIFY
                    ? Set.of("plan_verify") : Set.of();
        return HarnessPlanToolDescriptors.all().stream()
            .filter(descriptor -> allowed.contains(descriptor.toolName()))
            .toList();
    }
}
