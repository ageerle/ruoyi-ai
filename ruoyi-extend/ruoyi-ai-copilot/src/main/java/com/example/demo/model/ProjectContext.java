package com.example.demo.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Project context information
 * Contains complete project analysis results for AI understanding
 */
public class ProjectContext {
    private Path projectRoot;
    private ProjectType projectType;
    private ProjectStructure projectStructure;
    private List<DependencyInfo> dependencies;
    private List<ConfigFile> configFiles;
    private CodeStatistics codeStatistics;
    private Map<String, Object> metadata;
    private String contextSummary;
    
    public ProjectContext() {
        this.dependencies = new ArrayList<>();
        this.configFiles = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    public ProjectContext(Path projectRoot) {
        this();
        this.projectRoot = projectRoot;
    }
    


    /**
     * Dependency information class
     */
    public static class DependencyInfo {
        private String name;
        private String version;
        private String type; // "compile", "test", "runtime", etc.
        private String scope;
        private boolean isDirectDependency;
        
        public DependencyInfo(String name, String version, String type) {
            this.name = name;
            this.version = version;
            this.type = type;
            this.isDirectDependency = true;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        
        public boolean isDirectDependency() { return isDirectDependency; }
        public void setDirectDependency(boolean directDependency) { isDirectDependency = directDependency; }
        
        @Override
        public String toString() {
            return String.format("%s:%s (%s)", name, version, type);
        }
    }
    
    /**
     * Configuration file information class
     */
    public static class ConfigFile {
        private String fileName;
        private String relativePath;
        private String fileType; // "properties", "yaml", "json", "xml", etc.
        private Map<String, Object> keySettings;
        private boolean isMainConfig;
        
        public ConfigFile(String fileName, String relativePath, String fileType) {
            this.fileName = fileName;
            this.relativePath = relativePath;
            this.fileType = fileType;
            this.keySettings = new HashMap<>();
            this.isMainConfig = false;
        }
        
        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getRelativePath() { return relativePath; }
        public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
        
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public Map<String, Object> getKeySettings() { return keySettings; }
        public void setKeySettings(Map<String, Object> keySettings) { this.keySettings = keySettings; }
        
        public boolean isMainConfig() { return isMainConfig; }
        public void setMainConfig(boolean mainConfig) { isMainConfig = mainConfig; }
        
        public void addSetting(String key, Object value) {
            this.keySettings.put(key, value);
        }
    }
    
    /**
     * Code statistics information class
     */
    public static class CodeStatistics {
        private int totalLines;
        private int codeLines;
        private int commentLines;
        private int blankLines;
        private Map<String, Integer> languageLines;
        private int totalClasses;
        private int totalMethods;
        private int totalFunctions;
        
        public CodeStatistics() {
            this.languageLines = new HashMap<>();
        }
        
        // Getters and Setters
        public int getTotalLines() { return totalLines; }
        public void setTotalLines(int totalLines) { this.totalLines = totalLines; }
        
        public int getCodeLines() { return codeLines; }
        public void setCodeLines(int codeLines) { this.codeLines = codeLines; }
        
        public int getCommentLines() { return commentLines; }
        public void setCommentLines(int commentLines) { this.commentLines = commentLines; }
        
        public int getBlankLines() { return blankLines; }
        public void setBlankLines(int blankLines) { this.blankLines = blankLines; }
        
        public Map<String, Integer> getLanguageLines() { return languageLines; }
        public void setLanguageLines(Map<String, Integer> languageLines) { this.languageLines = languageLines; }
        
        public int getTotalClasses() { return totalClasses; }
        public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }
        
        public int getTotalMethods() { return totalMethods; }
        public void setTotalMethods(int totalMethods) { this.totalMethods = totalMethods; }
        
        public int getTotalFunctions() { return totalFunctions; }
        public void setTotalFunctions(int totalFunctions) { this.totalFunctions = totalFunctions; }
        
        public void addLanguageLines(String language, int lines) {
            this.languageLines.put(language, this.languageLines.getOrDefault(language, 0) + lines);
        }
    }
    
    /**
     * Generate project context summary
     */
    public String generateContextSummary() {
        StringBuilder summary = new StringBuilder();
        
        // Basic information
        summary.append("=== PROJECT CONTEXT ===\n");
        summary.append("Project: ").append(projectRoot != null ? projectRoot.getFileName() : "Unknown").append("\n");
        summary.append("Type: ").append(projectType != null ? projectType.getDisplayName() : "Unknown").append("\n");
        summary.append("Language: ").append(projectType != null ? projectType.getPrimaryLanguage() : "Unknown").append("\n");
        summary.append("Package Manager: ").append(projectType != null ? projectType.getPackageManager() : "Unknown").append("\n\n");

        // Project structure
        if (projectStructure != null) {
            summary.append("=== PROJECT STRUCTURE ===\n");
            summary.append(projectStructure.getStructureSummary()).append("\n");
        }

        // Dependencies
        if (!dependencies.isEmpty()) {
            summary.append("=== DEPENDENCIES ===\n");
            dependencies.stream()
                .filter(DependencyInfo::isDirectDependency)
                .limit(10)
                .forEach(dep -> summary.append("- ").append(dep.toString()).append("\n"));
            if (dependencies.size() > 10) {
                summary.append("... and ").append(dependencies.size() - 10).append(" more dependencies\n");
            }
            summary.append("\n");
        }

        // Configuration files
        if (!configFiles.isEmpty()) {
            summary.append("=== CONFIGURATION FILES ===\n");
            configFiles.stream()
                .filter(ConfigFile::isMainConfig)
                .forEach(config -> summary.append("- ").append(config.getFileName())
                    .append(" (").append(config.getFileType()).append(")\n"));
            summary.append("\n");
        }

        // Code statistics
        if (codeStatistics != null) {
            summary.append("=== CODE STATISTICS ===\n");
            summary.append("Total Lines: ").append(codeStatistics.getTotalLines()).append("\n");
            summary.append("Code Lines: ").append(codeStatistics.getCodeLines()).append("\n");
            if (codeStatistics.getTotalClasses() > 0) {
                summary.append("Classes: ").append(codeStatistics.getTotalClasses()).append("\n");
            }
            if (codeStatistics.getTotalMethods() > 0) {
                summary.append("Methods: ").append(codeStatistics.getTotalMethods()).append("\n");
            }
            summary.append("\n");
        }
        
        this.contextSummary = summary.toString();
        return this.contextSummary;
    }
    
    /**
     * Get dependency summary
     */
    public String getDependencySummary() {
        if (dependencies.isEmpty()) {
            return "No dependencies found";
        }
        
        return dependencies.stream()
            .filter(DependencyInfo::isDirectDependency)
            .limit(5)
            .map(DependencyInfo::getName)
            .reduce((a, b) -> a + ", " + b)
            .orElse("No direct dependencies");
    }
    
    // Getters and Setters
    public Path getProjectRoot() { return projectRoot; }
    public void setProjectRoot(Path projectRoot) { this.projectRoot = projectRoot; }
    
    public ProjectType getProjectType() { return projectType; }
    public void setProjectType(ProjectType projectType) { this.projectType = projectType; }
    
    public ProjectStructure getProjectStructure() { return projectStructure; }
    public void setProjectStructure(ProjectStructure projectStructure) { this.projectStructure = projectStructure; }
    
    public List<DependencyInfo> getDependencies() { return dependencies; }
    public void setDependencies(List<DependencyInfo> dependencies) { this.dependencies = dependencies; }
    
    public List<ConfigFile> getConfigFiles() { return configFiles; }
    public void setConfigFiles(List<ConfigFile> configFiles) { this.configFiles = configFiles; }
    
    public CodeStatistics getCodeStatistics() { return codeStatistics; }
    public void setCodeStatistics(CodeStatistics codeStatistics) { this.codeStatistics = codeStatistics; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public String getContextSummary() { return contextSummary; }
    public void setContextSummary(String contextSummary) { this.contextSummary = contextSummary; }
}
