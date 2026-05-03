package org.ruoyi.mcp.tools;

import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务规划工具
 * 将自然语言开发任务转换为可执行的结构化计划
 */
@Component
public class TaskPlannerTool implements BuiltinToolProvider {

    private static final long DEFAULT_APPROVAL_TTL_SECONDS = 30 * 60;

    public static final String DESCRIPTION = "Creates a structured coding task plan from a natural language request. " +
        "Returns objective, constraints, steps with acceptance criteria, and risk level. " +
        "Use this tool before executing file or command operations.";

    @Tool(DESCRIPTION)
    public String planTask(String goal, String constraints, String executionMode) {
        if (goal == null || goal.trim().isEmpty()) {
            return "Error: goal cannot be empty";
        }

        String normalizedGoal = goal.trim();
        String normalizedConstraints = normalize(constraints);
        String mode = normalizeMode(executionMode);
        RiskLevel risk = detectRisk(normalizedGoal, normalizedConstraints);

        List<String> steps = buildSteps(normalizedGoal, mode, risk);
        List<String> acceptance = buildAcceptance(mode, risk);

        StringBuilder sb = new StringBuilder();
        sb.append("# Task Plan\n");
        sb.append("Objective: ").append(normalizedGoal).append("\n");
        sb.append("ExecutionMode: ").append(mode).append("\n");
        sb.append("RiskLevel: ").append(risk.name()).append("\n");
        if (!normalizedConstraints.isEmpty()) {
            sb.append("Constraints: ").append(normalizedConstraints).append("\n");
        }

        sb.append("\nSteps:\n");
        for (int i = 0; i < steps.size(); i++) {
            sb.append(i + 1).append(". ").append(steps.get(i)).append("\n");
        }

        sb.append("\nAcceptanceCriteria:\n");
        for (int i = 0; i < acceptance.size(); i++) {
            sb.append(i + 1).append(". ").append(acceptance.get(i)).append("\n");
        }

        String approvalToken = ApprovalTokenStore.issue(normalizedGoal, DEFAULT_APPROVAL_TTL_SECONDS);

        sb.append("\nExecutionApproval:\n");
        sb.append("1. ApprovalToken: ").append(approvalToken).append("\n");
        sb.append("2. ApprovalScope: ").append(normalizedGoal).append("\n");
        sb.append("3. TokenTTLSeconds: ").append(DEFAULT_APPROVAL_TTL_SECONDS).append("\n");
        sb.append("4. Run command tools only after explicit user confirmation\n");

        sb.append("\nSafetyGates:\n");
        sb.append("1. Only workspace-scoped file operations are allowed\n");
        sb.append("2. Only whitelisted commands are allowed\n");
        sb.append("3. Risky actions require explicit user confirmation\n");

        return sb.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeMode(String executionMode) {
        String mode = normalize(executionMode).toUpperCase();
        if (!"PLAN_ONLY".equals(mode) && !"PLAN_AND_EXECUTE".equals(mode)) {
            return "PLAN_ONLY";
        }
        return mode;
    }

    private RiskLevel detectRisk(String goal, String constraints) {
        String text = (goal + " " + constraints).toLowerCase();
        if (containsAny(text, "delete", "drop", "remove", "reset --hard", "force", "rewrite history")) {
            return RiskLevel.HIGH;
        }
        if (containsAny(text, "refactor", "multi-module", "database", "migration", "deploy", "production")) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private List<String> buildSteps(String goal, String mode, RiskLevel risk) {
        List<String> steps = new ArrayList<>();
        steps.add("Clarify target scope and impacted modules for: " + goal);
        steps.add("Discover relevant files and APIs using list/read/search tools");
        steps.add("Design minimal change set and draft patch plan");
        steps.add("Apply file changes incrementally and keep each step reversible");
        steps.add("Run whitelisted validation commands (build/test/lint) for impacted modules only");
        if ("PLAN_AND_EXECUTE".equals(mode)) {
            steps.add("Prepare summary of changes and execution logs for user review");
        } else {
            steps.add("Return executable checklist and wait for execution approval");
        }
        if (risk != RiskLevel.LOW) {
            steps.add("Request explicit confirmation before any medium/high-risk operation");
        }
        return steps;
    }

    private List<String> buildAcceptance(String mode, RiskLevel risk) {
        List<String> criteria = new ArrayList<>();
        criteria.add("Planned steps are concrete, ordered, and testable");
        criteria.add("Every file/command action is bounded within workspace and policy constraints");
        criteria.add("Validation commands and expected outputs are specified");
        if ("PLAN_AND_EXECUTE".equals(mode)) {
            criteria.add("Executed changes produce a verifiable diff and command results");
        } else {
            criteria.add("Plan is ready for immediate execution after user approval");
        }
        if (risk == RiskLevel.HIGH) {
            criteria.add("High-risk actions are isolated and require explicit user confirmation");
        }
        return criteria;
    }

    @Override
    public String getToolName() {
        return "task_planner";
    }

    @Override
    public String getDisplayName() {
        return "任务规划";
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    private enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}
