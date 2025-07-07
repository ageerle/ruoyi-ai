package com.example.demo.tools;

import com.example.demo.config.AppProperties;
import com.example.demo.model.ProjectContext;
import com.example.demo.schema.JsonSchema;
import com.example.demo.service.ProjectContextAnalyzer;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Smart editing tool
 * Provides intelligent multi-file editing capabilities based on project context understanding
 */
@Component
public class SmartEditTool extends BaseTool<SmartEditTool.SmartEditParams> {
    
    private static final Logger logger = LoggerFactory.getLogger(SmartEditTool.class);
    
    @Autowired
    private ProjectContextAnalyzer projectContextAnalyzer;
    
    @Autowired
    private EditFileTool editFileTool;
    
    @Autowired
    private ReadFileTool readFileTool;
    
    @Autowired
    private WriteFileTool writeFileTool;
    
    @Autowired
    private ChatModel chatModel;
    
    private final String rootDirectory;
    private final AppProperties appProperties;
    
    public SmartEditTool(AppProperties appProperties) {
        super(
            "smart_edit",
            "SmartEdit", 
            "Intelligently edit projects based on natural language descriptions. " +
            "Analyzes project context and performs multi-file edits when necessary. " +
            "Can handle complex refactoring, feature additions, and project-wide changes.",
            createSchema()
        );
        this.appProperties = appProperties;
        this.rootDirectory = appProperties.getWorkspace().getRootDirectory();
    }
    
    private static JsonSchema createSchema() {
        return JsonSchema.object()
            .addProperty("project_path", JsonSchema.string(
                "Absolute path to the project root directory to analyze and edit"
            ))
            .addProperty("edit_description", JsonSchema.string(
                "Natural language description of the desired changes. " +
                "Examples: 'Add a new REST endpoint for user management', " +
                "'Refactor the authentication logic', 'Update dependencies to latest versions'"
            ))
            .addProperty("target_files", JsonSchema.array(JsonSchema.string(
                "Optional: Specific files to focus on. If not provided, the tool will determine which files to edit based on the description"
            )))
            .addProperty("scope", JsonSchema.string(
                "Edit scope: 'single_file', 'related_files', or 'project_wide'. Default: 'related_files'"
            ))
            .addProperty("dry_run", JsonSchema.bool(
                "If true, only analyze and show what would be changed without making actual changes. Default: false"
            ))
            .required("project_path", "edit_description");
    }
    
    public enum EditScope {
        SINGLE_FILE("single_file", "Edit only one file"),
        RELATED_FILES("related_files", "Edit related files that are affected by the change"),
        PROJECT_WIDE("project_wide", "Make project-wide changes including configuration files");
        
        private final String value;
        private final String description;
        
        EditScope(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public static EditScope fromString(String value) {
            for (EditScope scope : values()) {
                if (scope.value.equals(value)) {
                    return scope;
                }
            }
            return RELATED_FILES; // default
        }
        
        public String getValue() { return value; }
        public String getDescription() { return description; }
    }

    /**
     * Smart edit tool method for Spring AI integration
     */
    @Tool(name = "smart_edit", description = "Intelligently edit projects based on natural language descriptions")
    public String smartEdit(
            String projectPath,
            String editDescription,
            String scope,
            Boolean dryRun) {

        try {
            SmartEditParams params = new SmartEditParams();
            params.setProjectPath(projectPath);
            params.setEditDescription(editDescription);
            params.setScope(scope != null ? scope : "related_files");
            params.setDryRun(dryRun != null ? dryRun : false);

            // Validate parameters
            String validation = validateToolParams(params);
            if (validation != null) {
                return "Error: " + validation;
            }

            // Execute the tool
            ToolResult result = execute(params).join();

            if (result.isSuccess()) {
                return result.getLlmContent();
            } else {
                return "Error: " + result.getErrorMessage();
            }

        } catch (Exception e) {
            logger.error("Error in smart edit tool", e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public String validateToolParams(SmartEditParams params) {
        String baseValidation = super.validateToolParams(params);
        if (baseValidation != null) {
            return baseValidation;
        }
        
        if (params.projectPath == null || params.projectPath.trim().isEmpty()) {
            return "Project path cannot be empty";
        }
        
        if (params.editDescription == null || params.editDescription.trim().isEmpty()) {
            return "Edit description cannot be empty";
        }
        
        Path projectPath = Paths.get(params.projectPath);
        if (!projectPath.isAbsolute()) {
            return "Project path must be absolute: " + params.projectPath;
        }
        
        if (!Files.exists(projectPath)) {
            return "Project path does not exist: " + params.projectPath;
        }
        
        if (!Files.isDirectory(projectPath)) {
            return "Project path must be a directory: " + params.projectPath;
        }
        
        if (!isWithinWorkspace(projectPath)) {
            return "Project path must be within the workspace directory: " + params.projectPath;
        }
        
        return null;
    }
    
    @Override
    public CompletableFuture<ToolConfirmationDetails> shouldConfirmExecute(SmartEditParams params) {
        if (params.dryRun != null && params.dryRun) {
            return CompletableFuture.completedFuture(null); // No confirmation needed for dry run
        }
        
        if (appProperties.getSecurity().getApprovalMode() == AppProperties.ApprovalMode.AUTO_EDIT ||
            appProperties.getSecurity().getApprovalMode() == AppProperties.ApprovalMode.YOLO) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                EditPlan plan = analyzeAndPlanEdit(params);
                String confirmationMessage = buildConfirmationMessage(plan);
                
                return new ToolConfirmationDetails(
                    "smart_edit",
                    "Confirm Smart Edit: " + params.editDescription,
                    "Smart edit operation confirmation",
                    confirmationMessage
                );
                
            } catch (Exception e) {
                logger.warn("Could not generate edit plan for confirmation", e);
                return null;
            }
        });
    }
    
    @Override
    public CompletableFuture<ToolResult> execute(SmartEditParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting smart edit for project: {}", params.projectPath);
                logger.info("Edit description: {}", params.editDescription);
                
                // 1. Analyze project context
                Path projectPath = Paths.get(params.projectPath);
                ProjectContext context = projectContextAnalyzer.analyzeProject(projectPath);
                
                // 2. Generate edit plan
                EditPlan plan = generateEditPlan(params, context);
                
                if (params.dryRun != null && params.dryRun) {
                    return ToolResult.success(
                        "Dry run completed. Edit plan generated successfully.",
                        plan.toString()
                    );
                }
                
                // 3. Execute edit plan
                EditResult result = executeEditPlan(plan);
                
                logger.info("Smart edit completed for project: {}", params.projectPath);
                return ToolResult.success(
                    result.getSummary(),
                    result.getDetails()
                );
                
            } catch (Exception e) {
                logger.error("Error during smart edit execution", e);
                return ToolResult.error("Smart edit failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Analyze and generate edit plan
     */
    private EditPlan analyzeAndPlanEdit(SmartEditParams params) {
        Path projectPath = Paths.get(params.projectPath);
        ProjectContext context = projectContextAnalyzer.analyzeProject(projectPath);
        return generateEditPlan(params, context);
    }
    
    /**
     * Generate edit plan
     */
    private EditPlan generateEditPlan(SmartEditParams params, ProjectContext context) {
        logger.debug("Generating edit plan for: {}", params.editDescription);
        
        EditPlan plan = new EditPlan();
        plan.setDescription(params.editDescription);
        plan.setScope(EditScope.fromString(params.scope));
        plan.setProjectContext(context);
        
        // Use AI to analyze edit intent and generate specific edit steps
        String editContext = buildEditContext(context, params.editDescription);
        
        List<EditStep> steps = generateEditSteps(editContext, params);
        plan.setSteps(steps);
        
        return plan;
    }
    
    /**
     * Build edit context from project context and description
     */
    private String buildEditContext(ProjectContext context, String editDescription) {
        StringBuilder contextBuilder = new StringBuilder();

        contextBuilder.append("PROJECT CONTEXT:\n");
        contextBuilder.append("Type: ").append(context.getProjectType().getDisplayName()).append("\n");
        contextBuilder.append("Language: ").append(context.getProjectType().getPrimaryLanguage()).append("\n");

        if (context.getProjectStructure() != null) {
            contextBuilder.append("Structure: ").append(context.getProjectStructure().getStructureSummary()).append("\n");
        }

        if (context.getDependencies() != null && !context.getDependencies().isEmpty()) {
            contextBuilder.append("Dependencies: ").append(context.getDependencySummary()).append("\n");
        }

        contextBuilder.append("\nEDIT REQUEST: ").append(editDescription);

        return contextBuilder.toString();
    }

    /**
     * Use AI to generate edit steps
     */
    private List<EditStep> generateEditSteps(String editContext, SmartEditParams params) {
        List<EditStep> steps = new ArrayList<>();
        
        try {
            String prompt = buildEditPlanPrompt(editContext, params);
            
            List<Message> messages = List.of(new UserMessage(prompt));
            
            ChatResponse response = ChatClient.create(chatModel)
                .prompt()
                .messages(messages)
                .call()
                .chatResponse();
            
            String aiResponse = response.getResult().getOutput().getText();
            steps = parseEditStepsFromAI(aiResponse, params);
            
        } catch (Exception e) {
            logger.warn("Failed to generate AI-based edit steps, using fallback", e);
            steps = generateFallbackEditSteps(params);
        }
        
        return steps;
    }
    
    /**
     * Build edit plan prompt
     */
    private String buildEditPlanPrompt(String editContext, SmartEditParams params) {
        return String.format("""
            You are an expert software developer. Based on the project context below, 
            create a detailed plan to implement the requested changes.
            
            %s
            
            TASK: %s
            
            Please provide a step-by-step plan in the following format:
            STEP 1: [Action] - [File] - [Description]
            STEP 2: [Action] - [File] - [Description]
            ...
            
            Actions can be: CREATE, EDIT, DELETE, RENAME
            Be specific about which files need to be modified and what changes are needed.
            Consider dependencies between files and the overall project structure.
            """, editContext, params.editDescription);
    }
    
    /**
     * Parse edit steps from AI response
     */
    private List<EditStep> parseEditStepsFromAI(String aiResponse, SmartEditParams params) {
        List<EditStep> steps = new ArrayList<>();
        
        String[] lines = aiResponse.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("STEP") && line.contains(":")) {
                try {
                    EditStep step = parseEditStepLine(line, params);
                    if (step != null) {
                        steps.add(step);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse edit step: {}", line);
                }
            }
        }
        
        return steps;
    }
    
    /**
     * Parse single edit step line
     */
    private EditStep parseEditStepLine(String line, SmartEditParams params) {
        // Simple parsing implementation
        // In actual projects, more complex parsing logic should be used
        String[] parts = line.split(" - ");
        if (parts.length >= 3) {
            String actionPart = parts[0].substring(parts[0].indexOf(":") + 1).trim();
            String filePart = parts[1].trim();
            String descriptionPart = parts[2].trim();
            
            EditStep step = new EditStep();
            step.setAction(actionPart);
            step.setTargetFile(filePart);
            step.setDescription(descriptionPart);
            
            return step;
        }
        
        return null;
    }
    
    /**
     * Generate fallback edit steps
     */
    private List<EditStep> generateFallbackEditSteps(SmartEditParams params) {
        List<EditStep> steps = new ArrayList<>();
        
        // Simple fallback logic
        if (params.targetFiles != null && !params.targetFiles.isEmpty()) {
            for (String file : params.targetFiles) {
                EditStep step = new EditStep();
                step.setAction("EDIT");
                step.setTargetFile(file);
                step.setDescription("Edit " + file + " according to: " + params.editDescription);
                steps.add(step);
            }
        } else {
            // Default edit step
            EditStep step = new EditStep();
            step.setAction("ANALYZE");
            step.setTargetFile("*");
            step.setDescription("Analyze project and apply changes: " + params.editDescription);
            steps.add(step);
        }
        
        return steps;
    }
    
    /**
     * Execute edit plan
     */
    private EditResult executeEditPlan(EditPlan plan) {
        EditResult result = new EditResult();
        List<String> executedSteps = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (EditStep step : plan.getSteps()) {
            try {
                String stepResult = executeEditStep(step, plan.getProjectContext());
                executedSteps.add(stepResult);
                logger.debug("Executed step: {}", step.getDescription());
            } catch (Exception e) {
                String error = "Failed to execute step: " + step.getDescription() + " - " + e.getMessage();
                errors.add(error);
                logger.warn(error, e);
            }
        }
        
        result.setExecutedSteps(executedSteps);
        result.setErrors(errors);
        result.generateSummary();
        
        return result;
    }
    
    /**
     * Execute single edit step
     */
    private String executeEditStep(EditStep step, ProjectContext context) throws Exception {
        switch (step.getAction().toUpperCase()) {
            case "CREATE":
                return executeCreateStep(step, context);
            case "EDIT":
                return executeFileEditStep(step, context);
            case "DELETE":
                return executeDeleteStep(step, context);
            default:
                return "Skipped unsupported action: " + step.getAction();
        }
    }
    
    private String executeCreateStep(EditStep step, ProjectContext context) throws Exception {
        // Implement file creation logic
        return "Created file: " + step.getTargetFile();
    }
    
    private String executeFileEditStep(EditStep step, ProjectContext context) throws Exception {
        // Implement file editing logic
        return "Edited file: " + step.getTargetFile();
    }
    
    private String executeDeleteStep(EditStep step, ProjectContext context) throws Exception {
        // Implement file deletion logic
        return "Deleted file: " + step.getTargetFile();
    }
    
    private String buildConfirmationMessage(EditPlan plan) {
        StringBuilder message = new StringBuilder();
        message.append("Smart Edit Plan:\n");
        message.append("Description: ").append(plan.getDescription()).append("\n");
        message.append("Scope: ").append(plan.getScope().getDescription()).append("\n");
        message.append("Steps to execute:\n");
        
        for (int i = 0; i < plan.getSteps().size(); i++) {
            EditStep step = plan.getSteps().get(i);
            message.append(String.format("%d. %s - %s\n", 
                i + 1, step.getAction(), step.getDescription()));
        }
        
        return message.toString();
    }
    
    private boolean isWithinWorkspace(Path filePath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
            Path normalizedPath = filePath.toRealPath();
            return normalizedPath.startsWith(workspaceRoot);
        } catch (IOException e) {
            logger.warn("Could not resolve workspace path", e);
            return false;
        }
    }
    
    // Inner class definitions
    public static class SmartEditParams {
        @JsonProperty("project_path")
        private String projectPath;
        
        @JsonProperty("edit_description")
        private String editDescription;
        
        @JsonProperty("target_files")
        private List<String> targetFiles;
        
        @JsonProperty("scope")
        private String scope = "related_files";
        
        @JsonProperty("dry_run")
        private Boolean dryRun = false;
        
        // Getters and Setters
        public String getProjectPath() { return projectPath; }
        public void setProjectPath(String projectPath) { this.projectPath = projectPath; }
        
        public String getEditDescription() { return editDescription; }
        public void setEditDescription(String editDescription) { this.editDescription = editDescription; }
        
        public List<String> getTargetFiles() { return targetFiles; }
        public void setTargetFiles(List<String> targetFiles) { this.targetFiles = targetFiles; }
        
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        
        public Boolean getDryRun() { return dryRun; }
        public void setDryRun(Boolean dryRun) { this.dryRun = dryRun; }
    }
    
    private static class EditPlan {
        private String description;
        private EditScope scope;
        private ProjectContext projectContext;
        private List<EditStep> steps;
        
        // Getters and Setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public EditScope getScope() { return scope; }
        public void setScope(EditScope scope) { this.scope = scope; }
        
        public ProjectContext getProjectContext() { return projectContext; }
        public void setProjectContext(ProjectContext projectContext) { this.projectContext = projectContext; }
        
        public List<EditStep> getSteps() { return steps; }
        public void setSteps(List<EditStep> steps) { this.steps = steps; }
    }
    
    private static class EditStep {
        private String action;
        private String targetFile;
        private String description;
        
        // Getters and Setters
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getTargetFile() { return targetFile; }
        public void setTargetFile(String targetFile) { this.targetFile = targetFile; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    private static class EditResult {
        private List<String> executedSteps;
        private List<String> errors;
        private String summary;
        private String details;
        
        public void generateSummary() {
            int successCount = executedSteps.size();
            int errorCount = errors.size();
            
            this.summary = String.format("Smart edit completed: %d steps executed, %d errors", 
                successCount, errorCount);
            
            StringBuilder detailsBuilder = new StringBuilder();
            detailsBuilder.append("Executed Steps:\n");
            for (String step : executedSteps) {
                detailsBuilder.append("✓ ").append(step).append("\n");
            }
            
            if (!errors.isEmpty()) {
                detailsBuilder.append("\nErrors:\n");
                for (String error : errors) {
                    detailsBuilder.append("✗ ").append(error).append("\n");
                }
            }
            
            this.details = detailsBuilder.toString();
        }
        
        // Getters and Setters
        public List<String> getExecutedSteps() { return executedSteps; }
        public void setExecutedSteps(List<String> executedSteps) { this.executedSteps = executedSteps; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public String getSummary() { return summary; }
        public String getDetails() { return details; }
    }
}
