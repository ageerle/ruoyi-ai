package com.example.demo.tools;

import com.example.demo.config.AppProperties;
import com.example.demo.model.ProjectContext;
import com.example.demo.model.ProjectStructure;
import com.example.demo.schema.JsonSchema;
import com.example.demo.service.ProjectContextAnalyzer;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * 项目分析工具
 * 分析现有项目的结构、类型、依赖等信息
 */
@Component
public class AnalyzeProjectTool extends BaseTool<AnalyzeProjectTool.AnalyzeProjectParams> {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeProjectTool.class);
    
    @Autowired
    private ProjectContextAnalyzer projectContextAnalyzer;
    
    private final String rootDirectory;
    private final AppProperties appProperties;
    
    public AnalyzeProjectTool(AppProperties appProperties) {
        super(
            "analyze_project",
            "AnalyzeProject",
            "Analyze an existing project to understand its structure, type, dependencies, and configuration. " +
            "Provides comprehensive project information that can be used for intelligent editing and refactoring.",
            createSchema()
        );
        this.appProperties = appProperties;
        this.rootDirectory = appProperties.getWorkspace().getRootDirectory();
    }
    
    private static JsonSchema createSchema() {
        return JsonSchema.object()
            .addProperty("project_path", JsonSchema.string(
                "Absolute path to the project root directory to analyze. " +
                "Must be within the workspace directory."
            ))
            .addProperty("analysis_depth", JsonSchema.string(
                "Analysis depth: 'basic', 'detailed', or 'comprehensive'. " +
                "Default: 'detailed'. " +
                "- basic: Project type and structure only\n" +
                "- detailed: Includes dependencies and configuration\n" +
                "- comprehensive: Full analysis including code statistics"
            ))
            .addProperty("include_code_stats", JsonSchema.bool(
                "Whether to include detailed code statistics (lines of code, classes, methods, etc.). " +
                "Default: true for detailed/comprehensive analysis"
            ))
            .addProperty("output_format", JsonSchema.string(
                "Output format: 'summary', 'detailed', or 'json'. Default: 'detailed'"
            ))
            .required("project_path");
    }
    
    public enum AnalysisDepth {
        BASIC("basic", "Basic project type and structure analysis"),
        DETAILED("detailed", "Detailed analysis including dependencies and configuration"),
        COMPREHENSIVE("comprehensive", "Comprehensive analysis with full code statistics");
        
        private final String value;
        private final String description;
        
        AnalysisDepth(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public static AnalysisDepth fromString(String value) {
            for (AnalysisDepth depth : values()) {
                if (depth.value.equals(value)) {
                    return depth;
                }
            }
            return DETAILED; // default
        }
        
        public String getValue() { return value; }
        public String getDescription() { return description; }
    }
    
    public enum OutputFormat {
        SUMMARY("summary", "Brief summary of key project information"),
        DETAILED("detailed", "Detailed human-readable analysis report"),
        JSON("json", "Structured JSON output for programmatic use");
        
        private final String value;
        private final String description;
        
        OutputFormat(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public static OutputFormat fromString(String value) {
            for (OutputFormat format : values()) {
                if (format.value.equals(value)) {
                    return format;
                }
            }
            return DETAILED; // default
        }
        
        public String getValue() { return value; }
        public String getDescription() { return description; }
    }
    
    @Override
    public String validateToolParams(AnalyzeProjectParams params) {
        String baseValidation = super.validateToolParams(params);
        if (baseValidation != null) {
            return baseValidation;
        }
        
        if (params.projectPath == null || params.projectPath.trim().isEmpty()) {
            return "Project path cannot be empty";
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

    /**
     * Analyze project tool method for Spring AI integration
     */
    @Tool(name = "analyze_project", description = "Analyzes project structure, type, dependencies and other information")
    public String analyzeProject(String projectPath, String analysisDepth, String outputFormat, Boolean includeCodeStats) {
        try {
            AnalyzeProjectParams params = new AnalyzeProjectParams();
            params.setProjectPath(projectPath);
            params.setAnalysisDepth(analysisDepth != null ? analysisDepth : "basic");
            params.setOutputFormat(outputFormat != null ? outputFormat : "detailed");
            params.setIncludeCodeStats(includeCodeStats != null ? includeCodeStats : false);

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
            logger.error("Error in analyze project tool", e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public CompletableFuture<ToolResult> execute(AnalyzeProjectParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting project analysis for: {}", params.projectPath);
                
                Path projectPath = Paths.get(params.projectPath);
                AnalysisDepth depth = AnalysisDepth.fromString(params.analysisDepth);
                OutputFormat format = OutputFormat.fromString(params.outputFormat);
                
                // 执行项目分析
                ProjectContext context = analyzeProject(projectPath, depth, params);
                
                // 生成输出
                String output = generateOutput(context, format, depth);
                String summary = generateSummary(context);
                
                logger.info("Project analysis completed for: {}", params.projectPath);
                return ToolResult.success(summary, output);
                
            } catch (Exception e) {
                logger.error("Error during project analysis", e);
                return ToolResult.error("Project analysis failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * 执行项目分析
     */
    private ProjectContext analyzeProject(Path projectPath, AnalysisDepth depth, AnalyzeProjectParams params) {
        logger.debug("Analyzing project with depth: {}", depth);
        
        switch (depth) {
            case BASIC:
                return analyzeBasic(projectPath);
            case DETAILED:
                return analyzeDetailed(projectPath, params);
            case COMPREHENSIVE:
                return analyzeComprehensive(projectPath, params);
            default:
                return projectContextAnalyzer.analyzeProject(projectPath);
        }
    }
    
    /**
     * 基础分析
     */
    private ProjectContext analyzeBasic(Path projectPath) {
        // 只分析项目类型和基本结构
        ProjectContext context = new ProjectContext(projectPath);
        context.setProjectType(projectContextAnalyzer.projectTypeDetector.detectProjectType(projectPath));
        context.setProjectStructure(projectContextAnalyzer.projectDiscoveryService.analyzeProjectStructure(projectPath));
        return context;
    }
    
    /**
     * 详细分析
     */
    private ProjectContext analyzeDetailed(Path projectPath, AnalyzeProjectParams params) {
        ProjectContext context = analyzeBasic(projectPath);
        
        // 添加依赖和配置文件分析
        context.setDependencies(projectContextAnalyzer.projectDiscoveryService.analyzeDependencies(projectPath));
        context.setConfigFiles(projectContextAnalyzer.projectDiscoveryService.findConfigurationFiles(projectPath));
        
        // 如果需要代码统计
        if (params.includeCodeStats == null || params.includeCodeStats) {
            // 简化的代码统计，避免性能问题
            ProjectContext.CodeStatistics stats = new ProjectContext.CodeStatistics();
            // 这里可以添加基本的代码统计逻辑
            context.setCodeStatistics(stats);
        }
        
        return context;
    }
    
    /**
     * 全面分析
     */
    private ProjectContext analyzeComprehensive(Path projectPath, AnalyzeProjectParams params) {
        // 使用完整的项目分析
        return projectContextAnalyzer.analyzeProject(projectPath);
    }
    
    /**
     * 生成输出
     */
    private String generateOutput(ProjectContext context, OutputFormat format, AnalysisDepth depth) {
        switch (format) {
            case SUMMARY:
                return generateSummaryOutput(context);
            case DETAILED:
                return generateDetailedOutput(context, depth);
            case JSON:
                return generateJsonOutput(context);
            default:
                return generateDetailedOutput(context, depth);
        }
    }
    
    /**
     * 生成摘要输出
     */
    private String generateSummaryOutput(ProjectContext context) {
        StringBuilder output = new StringBuilder();
        
        output.append("📊 PROJECT ANALYSIS SUMMARY\n");
        output.append("=" .repeat(50)).append("\n\n");
        
        // 基本信息
        output.append("🏗️  Project: ").append(context.getProjectRoot().getFileName()).append("\n");
        output.append("🔧 Type: ").append(context.getProjectType().getDisplayName()).append("\n");
        output.append("💻 Language: ").append(context.getProjectType().getPrimaryLanguage()).append("\n");
        output.append("📦 Package Manager: ").append(context.getProjectType().getPackageManager()).append("\n\n");
        
        // 结构信息
        if (context.getProjectStructure() != null) {
            ProjectStructure structure = context.getProjectStructure();
            output.append("📁 Structure:\n");
            output.append("   - Directories: ").append(structure.getTotalDirectories()).append("\n");
            output.append("   - Files: ").append(structure.getTotalFiles()).append("\n");
            output.append("   - Size: ").append(formatFileSize(structure.getTotalSize())).append("\n\n");
        }
        
        // 依赖信息
        if (context.getDependencies() != null && !context.getDependencies().isEmpty()) {
            output.append("📚 Dependencies: ").append(context.getDependencies().size()).append(" found\n");
            output.append("   - Key dependencies: ").append(context.getDependencySummary()).append("\n\n");
        }
        
        // 配置文件
        if (context.getConfigFiles() != null && !context.getConfigFiles().isEmpty()) {
            output.append("⚙️  Configuration Files: ").append(context.getConfigFiles().size()).append(" found\n");
            context.getConfigFiles().stream()
                .filter(ProjectContext.ConfigFile::isMainConfig)
                .forEach(config -> output.append("   - ").append(config.getFileName()).append("\n"));
        }
        
        return output.toString();
    }
    
    /**
     * 生成详细输出
     */
    private String generateDetailedOutput(ProjectContext context, AnalysisDepth depth) {
        StringBuilder output = new StringBuilder();
        
        output.append("📊 COMPREHENSIVE PROJECT ANALYSIS\n");
        output.append("=" .repeat(60)).append("\n\n");
        
        // 使用项目上下文的摘要生成功能
        output.append(context.generateContextSummary());
        
        // 添加分析深度特定的信息
        if (depth == AnalysisDepth.COMPREHENSIVE) {
            output.append("\n=== DETAILED INSIGHTS ===\n");
            output.append(generateProjectInsights(context));
        }
        
        return output.toString();
    }
    
    /**
     * 生成JSON输出
     */
    private String generateJsonOutput(ProjectContext context) {
        // 简化的JSON输出实现
        // 在实际项目中应该使用Jackson等JSON库
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"projectName\": \"").append(context.getProjectRoot().getFileName()).append("\",\n");
        json.append("  \"projectType\": \"").append(context.getProjectType().name()).append("\",\n");
        json.append("  \"primaryLanguage\": \"").append(context.getProjectType().getPrimaryLanguage()).append("\",\n");
        
        if (context.getProjectStructure() != null) {
            ProjectStructure structure = context.getProjectStructure();
            json.append("  \"structure\": {\n");
            json.append("    \"directories\": ").append(structure.getTotalDirectories()).append(",\n");
            json.append("    \"files\": ").append(structure.getTotalFiles()).append(",\n");
            json.append("    \"totalSize\": ").append(structure.getTotalSize()).append("\n");
            json.append("  },\n");
        }
        
        json.append("  \"dependencyCount\": ").append(
            context.getDependencies() != null ? context.getDependencies().size() : 0).append(",\n");
        json.append("  \"configFileCount\": ").append(
            context.getConfigFiles() != null ? context.getConfigFiles().size() : 0).append("\n");
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * 生成项目洞察
     */
    private String generateProjectInsights(ProjectContext context) {
        StringBuilder insights = new StringBuilder();
        
        // 项目健康度评估
        insights.append("Project Health Assessment:\n");
        
        // 检查是否有版本控制
        if (context.getMetadata().containsKey("versionControl")) {
            insights.append("✅ Version control detected: ").append(context.getMetadata().get("versionControl")).append("\n");
        } else {
            insights.append("⚠️  No version control detected\n");
        }
        
        // 检查是否有CI/CD
        if (context.getMetadata().containsKey("cicd")) {
            insights.append("✅ CI/CD configured: ").append(context.getMetadata().get("cicd")).append("\n");
        } else {
            insights.append("💡 Consider setting up CI/CD\n");
        }
        
        // 检查是否有容器化
        if (context.getMetadata().containsKey("containerization")) {
            insights.append("✅ Containerization: ").append(context.getMetadata().get("containerization")).append("\n");
        }
        
        // 代码质量建议
        insights.append("\nRecommendations:\n");
        if (context.getProjectType().isJavaProject()) {
            insights.append("- Consider using static analysis tools like SpotBugs or PMD\n");
            insights.append("- Ensure proper test coverage with JUnit\n");
        } else if (context.getProjectType().isJavaScriptProject()) {
            insights.append("- Consider using ESLint for code quality\n");
            insights.append("- Add TypeScript for better type safety\n");
        } else if (context.getProjectType().isPythonProject()) {
            insights.append("- Consider using pylint or flake8 for code quality\n");
            insights.append("- Add type hints for better code documentation\n");
        }
        
        return insights.toString();
    }
    
    /**
     * 生成摘要
     */
    private String generateSummary(ProjectContext context) {
        return String.format("Analyzed %s project: %s (%s) with %d dependencies and %d config files",
            context.getProjectType().getDisplayName(),
            context.getProjectRoot().getFileName(),
            context.getProjectType().getPrimaryLanguage(),
            context.getDependencies() != null ? context.getDependencies().size() : 0,
            context.getConfigFiles() != null ? context.getConfigFiles().size() : 0
        );
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    /**
     * 检查路径是否在工作空间内
     */
    private boolean isWithinWorkspace(Path filePath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
            Path normalizedPath = filePath.toRealPath();
            return normalizedPath.startsWith(workspaceRoot);
        } catch (Exception e) {
            logger.warn("Could not resolve workspace path", e);
            return false;
        }
    }
    
    /**
     * 分析项目参数
     */
    public static class AnalyzeProjectParams {
        @JsonProperty("project_path")
        private String projectPath;
        
        @JsonProperty("analysis_depth")
        private String analysisDepth = "detailed";
        
        @JsonProperty("include_code_stats")
        private Boolean includeCodeStats;
        
        @JsonProperty("output_format")
        private String outputFormat = "detailed";
        
        // Getters and Setters
        public String getProjectPath() { return projectPath; }
        public void setProjectPath(String projectPath) { this.projectPath = projectPath; }
        
        public String getAnalysisDepth() { return analysisDepth; }
        public void setAnalysisDepth(String analysisDepth) { this.analysisDepth = analysisDepth; }
        
        public Boolean getIncludeCodeStats() { return includeCodeStats; }
        public void setIncludeCodeStats(Boolean includeCodeStats) { this.includeCodeStats = includeCodeStats; }
        
        public String getOutputFormat() { return outputFormat; }
        public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
        
        @Override
        public String toString() {
            return String.format("AnalyzeProjectParams{path='%s', depth='%s', format='%s'}", 
                projectPath, analysisDepth, outputFormat);
        }
    }
}
