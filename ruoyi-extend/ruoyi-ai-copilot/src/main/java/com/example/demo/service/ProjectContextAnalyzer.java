package com.example.demo.service;

import com.example.demo.model.ProjectContext;
import com.example.demo.model.ProjectStructure;
import com.example.demo.model.ProjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 项目上下文分析器
 * 提供完整的项目分析功能，生成AI可理解的项目上下文
 */
@Service
public class ProjectContextAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectContextAnalyzer.class);
    
    @Autowired
    public ProjectTypeDetector projectTypeDetector;

    @Autowired
    public ProjectDiscoveryService projectDiscoveryService;
    
    /**
     * 分析项目并生成完整上下文
     * @param projectRoot 项目根目录
     * @return 项目上下文信息
     */
    public ProjectContext analyzeProject(Path projectRoot) {
        logger.info("Starting comprehensive project analysis for: {}", projectRoot);
        
        ProjectContext context = new ProjectContext(projectRoot);
        
        try {
            // 1. 检测项目类型
            ProjectType projectType = projectTypeDetector.detectProjectType(projectRoot);
            context.setProjectType(projectType);
            logger.debug("Detected project type: {}", projectType);
            
            // 2. 分析项目结构
            ProjectStructure structure = projectDiscoveryService.analyzeProjectStructure(projectRoot);
            context.setProjectStructure(structure);
            logger.debug("Analyzed project structure with {} directories", 
                structure.getDirectories().size());
            
            // 3. 分析依赖关系
            List<ProjectContext.DependencyInfo> dependencies = 
                projectDiscoveryService.analyzeDependencies(projectRoot);
            context.setDependencies(dependencies);
            logger.debug("Found {} dependencies", dependencies.size());
            
            // 4. 查找配置文件
            List<ProjectContext.ConfigFile> configFiles = 
                projectDiscoveryService.findConfigurationFiles(projectRoot);
            context.setConfigFiles(configFiles);
            logger.debug("Found {} configuration files", configFiles.size());
            
            // 5. 分析代码统计
            ProjectContext.CodeStatistics codeStats = analyzeCodeStatistics(projectRoot, projectType);
            context.setCodeStatistics(codeStats);
            logger.debug("Code statistics: {} total lines", codeStats.getTotalLines());
            
            // 6. 收集项目元数据
            Map<String, Object> metadata = collectProjectMetadata(projectRoot, projectType);
            context.setMetadata(metadata);
            
            // 7. 生成上下文摘要
            String summary = context.generateContextSummary();
            logger.debug("Generated context summary with {} characters", summary.length());
            
            logger.info("Project analysis completed successfully for: {}", projectRoot);
            return context;
            
        } catch (Exception e) {
            logger.error("Error during project analysis for: " + projectRoot, e);
            // 返回部分分析结果
            return context;
        }
    }
    
    /**
     * 分析代码统计信息
     */
    private ProjectContext.CodeStatistics analyzeCodeStatistics(Path projectRoot, ProjectType projectType) {
        logger.debug("Analyzing code statistics for: {}", projectRoot);
        
        ProjectContext.CodeStatistics stats = new ProjectContext.CodeStatistics();
        
        try {
            analyzeCodeInDirectory(projectRoot, stats, projectType, 0, 3);
        } catch (Exception e) {
            logger.warn("Error analyzing code statistics", e);
        }
        
        return stats;
    }
    
    /**
     * 递归分析目录中的代码
     */
    private void analyzeCodeInDirectory(Path directory, ProjectContext.CodeStatistics stats, 
                                      ProjectType projectType, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth) {
            return;
        }
        
        try (Stream<Path> paths = Files.list(directory)) {
            paths.forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        String dirName = path.getFileName().toString();
                        // 跳过不需要分析的目录
                        if (!shouldSkipDirectory(dirName)) {
                            analyzeCodeInDirectory(path, stats, projectType, currentDepth + 1, maxDepth);
                        }
                    } else if (Files.isRegularFile(path)) {
                        analyzeCodeFile(path, stats, projectType);
                    }
                } catch (Exception e) {
                    logger.warn("Error processing path during code analysis: " + path, e);
                }
            });
        } catch (IOException e) {
            logger.warn("Error listing directory: " + directory, e);
        }
    }
    
    /**
     * 分析单个代码文件
     */
    private void analyzeCodeFile(Path filePath, ProjectContext.CodeStatistics stats, ProjectType projectType) {
        String fileName = filePath.getFileName().toString();
        String extension = getFileExtension(fileName).toLowerCase();
        
        // 只分析代码文件
        if (!isCodeFile(extension, projectType)) {
            return;
        }
        
        try {
            List<String> lines = Files.readAllLines(filePath);
            int totalLines = lines.size();
            int codeLines = 0;
            int commentLines = 0;
            int blankLines = 0;
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    blankLines++;
                } else if (isCommentLine(trimmedLine, extension)) {
                    commentLines++;
                } else {
                    codeLines++;
                }
            }
            
            // 更新统计信息
            stats.setTotalLines(stats.getTotalLines() + totalLines);
            stats.setCodeLines(stats.getCodeLines() + codeLines);
            stats.setCommentLines(stats.getCommentLines() + commentLines);
            stats.setBlankLines(stats.getBlankLines() + blankLines);
            
            // 按语言统计
            String language = getLanguageByExtension(extension);
            stats.addLanguageLines(language, totalLines);
            
            // 分析类和方法（简单实现）
            if (extension.equals(".java")) {
                analyzeJavaFile(lines, stats);
            } else if (extension.equals(".js") || extension.equals(".ts")) {
                analyzeJavaScriptFile(lines, stats);
            } else if (extension.equals(".py")) {
                analyzePythonFile(lines, stats);
            }
            
        } catch (IOException e) {
            logger.warn("Error reading file for code analysis: " + filePath, e);
        }
    }
    
    /**
     * 分析Java文件
     */
    private void analyzeJavaFile(List<String> lines, ProjectContext.CodeStatistics stats) {
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.matches(".*\\bclass\\s+\\w+.*")) {
                stats.setTotalClasses(stats.getTotalClasses() + 1);
            }
            if (trimmedLine.matches(".*\\b(public|private|protected)\\s+.*\\s+\\w+\\s*\\(.*\\).*")) {
                stats.setTotalMethods(stats.getTotalMethods() + 1);
            }
        }
    }
    
    /**
     * 分析JavaScript文件
     */
    private void analyzeJavaScriptFile(List<String> lines, ProjectContext.CodeStatistics stats) {
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.matches(".*\\bfunction\\s+\\w+.*") || 
                trimmedLine.matches(".*\\w+\\s*:\\s*function.*") ||
                trimmedLine.matches(".*\\w+\\s*=\\s*\\(.*\\)\\s*=>.*")) {
                stats.setTotalFunctions(stats.getTotalFunctions() + 1);
            }
        }
    }
    
    /**
     * 分析Python文件
     */
    private void analyzePythonFile(List<String> lines, ProjectContext.CodeStatistics stats) {
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.matches("^class\\s+\\w+.*:")) {
                stats.setTotalClasses(stats.getTotalClasses() + 1);
            }
            if (trimmedLine.matches("^def\\s+\\w+.*:")) {
                stats.setTotalFunctions(stats.getTotalFunctions() + 1);
            }
        }
    }
    
    /**
     * 收集项目元数据
     */
    private Map<String, Object> collectProjectMetadata(Path projectRoot, ProjectType projectType) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("projectName", projectRoot.getFileName().toString());
        metadata.put("projectType", projectType.name());
        metadata.put("primaryLanguage", projectType.getPrimaryLanguage());
        metadata.put("packageManager", projectType.getPackageManager());
        metadata.put("analysisTimestamp", System.currentTimeMillis());
        
        // 检查版本控制
        if (Files.exists(projectRoot.resolve(".git"))) {
            metadata.put("versionControl", "Git");
        }
        
        // 检查CI/CD配置
        if (Files.exists(projectRoot.resolve(".github"))) {
            metadata.put("cicd", "GitHub Actions");
        } else if (Files.exists(projectRoot.resolve(".gitlab-ci.yml"))) {
            metadata.put("cicd", "GitLab CI");
        }
        
        // 检查Docker支持
        if (Files.exists(projectRoot.resolve("Dockerfile"))) {
            metadata.put("containerization", "Docker");
        }
        
        return metadata;
    }
    
    /**
     * 生成编辑上下文
     */
    public String buildEditContext(Path projectRoot, String editDescription) {
        logger.debug("Building edit context for: {}", projectRoot);
        
        ProjectContext context = analyzeProject(projectRoot);
        
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("=== EDIT CONTEXT ===\n");
        contextBuilder.append("Edit Request: ").append(editDescription).append("\n\n");
        contextBuilder.append(context.generateContextSummary());
        
        return contextBuilder.toString();
    }
    
    // 辅助方法
    private boolean shouldSkipDirectory(String dirName) {
        return dirName.equals(".git") || dirName.equals("node_modules") || 
               dirName.equals("target") || dirName.equals("build") ||
               dirName.equals("dist") || dirName.equals("__pycache__") ||
               dirName.startsWith(".");
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }
    
    private boolean isCodeFile(String extension, ProjectType projectType) {
        return extension.equals(".java") || extension.equals(".js") || extension.equals(".ts") ||
               extension.equals(".py") || extension.equals(".html") || extension.equals(".css") ||
               extension.equals(".jsx") || extension.equals(".tsx") || extension.equals(".vue") ||
               extension.equals(".go") || extension.equals(".rs") || extension.equals(".php") ||
               extension.equals(".cs") || extension.equals(".cpp") || extension.equals(".c");
    }
    
    private boolean isCommentLine(String line, String extension) {
        switch (extension) {
            case ".java":
            case ".js":
            case ".ts":
            case ".jsx":
            case ".tsx":
            case ".css":
                return line.startsWith("//") || line.startsWith("/*") || line.startsWith("*");
            case ".py":
                return line.startsWith("#");
            case ".html":
                return line.startsWith("<!--");
            default:
                return line.startsWith("#") || line.startsWith("//");
        }
    }
    
    private String getLanguageByExtension(String extension) {
        switch (extension) {
            case ".java": return "Java";
            case ".js": case ".jsx": return "JavaScript";
            case ".ts": case ".tsx": return "TypeScript";
            case ".py": return "Python";
            case ".html": return "HTML";
            case ".css": return "CSS";
            case ".vue": return "Vue";
            case ".go": return "Go";
            case ".rs": return "Rust";
            case ".php": return "PHP";
            case ".cs": return "C#";
            default: return "Other";
        }
    }
}
