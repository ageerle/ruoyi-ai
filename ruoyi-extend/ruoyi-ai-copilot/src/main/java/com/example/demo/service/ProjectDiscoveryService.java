package com.example.demo.service;

import com.example.demo.model.ProjectContext;
import com.example.demo.model.ProjectStructure;
import com.example.demo.model.ProjectType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 项目发现和分析服务
 * 负责分析项目结构、依赖关系和配置信息
 */
@Service
public class ProjectDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectDiscoveryService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private ProjectTypeDetector projectTypeDetector;

    /**
     * 分析项目结构
     *
     * @param projectRoot 项目根目录
     * @return 项目结构信息
     */
    public ProjectStructure analyzeProjectStructure(Path projectRoot) {
        logger.debug("Analyzing project structure for: {}", projectRoot);

        ProjectType projectType = projectTypeDetector.detectProjectType(projectRoot);
        ProjectStructure structure = new ProjectStructure(projectRoot, projectType);

        try {
            analyzeDirectoryStructure(projectRoot, structure, 0, 3); // 最大深度3层
            structure.markImportantDirectories();

            logger.info("Project structure analysis completed for: {}", projectRoot);
            return structure;

        } catch (IOException e) {
            logger.error("Error analyzing project structure for: " + projectRoot, e);
            return structure; // 返回部分分析结果
        }
    }

    /**
     * 递归分析目录结构
     */
    private void analyzeDirectoryStructure(Path currentPath, ProjectStructure structure,
                                           int currentDepth, int maxDepth) throws IOException {
        if (currentDepth > maxDepth) {
            return;
        }

        try (Stream<Path> paths = Files.list(currentPath)) {
            paths.forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        String dirName = path.getFileName().toString();
                        String relativePath = structure.getProjectRoot().relativize(path).toString();

                        // 跳过常见的忽略目录
                        if (shouldIgnoreDirectory(dirName)) {
                            return;
                        }

                        ProjectStructure.DirectoryInfo dirInfo =
                                new ProjectStructure.DirectoryInfo(dirName, relativePath);

                        // 分析目录中的文件
                        analyzeDirectoryFiles(path, dirInfo);
                        structure.addDirectory(dirInfo);

                        // 递归分析子目录
                        if (currentDepth < maxDepth) {
                            analyzeDirectoryStructure(path, structure, currentDepth + 1, maxDepth);
                        }

                    } else if (Files.isRegularFile(path)) {
                        // 处理根目录下的文件
                        String fileName = path.getFileName().toString();
                        String extension = getFileExtension(fileName);

                        structure.addFileType(extension, 1);
                        structure.setTotalFiles(structure.getTotalFiles() + 1);

                        // 检查是否为关键文件
                        if (isKeyFile(fileName, structure.getProjectType())) {
                            structure.addKeyFile(fileName);
                        }

                        // 累计文件大小
                        try {
                            structure.setTotalSize(structure.getTotalSize() + Files.size(path));
                        } catch (IOException e) {
                            logger.warn("Could not get size for file: {}", path);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error processing path: " + path, e);
                }
            });
        }
    }

    /**
     * 分析目录中的文件
     */
    private void analyzeDirectoryFiles(Path directory, ProjectStructure.DirectoryInfo dirInfo) {
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .forEach(file -> {
                        String fileName = file.getFileName().toString();
                        dirInfo.addFile(fileName);
                    });
        } catch (IOException e) {
            logger.warn("Error analyzing files in directory: {}", directory);
        }
    }

    /**
     * 分析项目依赖
     */
    public List<ProjectContext.DependencyInfo> analyzeDependencies(Path projectRoot) {
        logger.debug("Analyzing dependencies for: {}", projectRoot);

        List<ProjectContext.DependencyInfo> dependencies = new ArrayList<>();
        ProjectType projectType = projectTypeDetector.detectProjectType(projectRoot);

        try {
            switch (projectType) {
                case JAVA_MAVEN:
                case SPRING_BOOT:
                    dependencies.addAll(analyzeMavenDependencies(projectRoot));
                    break;
                case NODE_JS:
                case REACT:
                case VUE:
                case ANGULAR:
                case NEXT_JS:
                    dependencies.addAll(analyzeNpmDependencies(projectRoot));
                    break;
                case PYTHON:
                case DJANGO:
                case FLASK:
                case FASTAPI:
                    dependencies.addAll(analyzePythonDependencies(projectRoot));
                    break;
                default:
                    logger.info("Dependency analysis not supported for project type: {}", projectType);
            }
        } catch (Exception e) {
            logger.error("Error analyzing dependencies for: " + projectRoot, e);
        }

        logger.info("Found {} dependencies for project: {}", dependencies.size(), projectRoot);
        return dependencies;
    }

    /**
     * 分析Maven依赖
     */
    private List<ProjectContext.DependencyInfo> analyzeMavenDependencies(Path projectRoot) {
        List<ProjectContext.DependencyInfo> dependencies = new ArrayList<>();
        Path pomFile = projectRoot.resolve("pom.xml");

        if (!Files.exists(pomFile)) {
            return dependencies;
        }

        try {
            String pomContent = Files.readString(pomFile);
            // 简单的XML解析 - 在实际项目中应该使用专门的XML解析器
            if (pomContent.contains("spring-boot-starter-web")) {
                dependencies.add(new ProjectContext.DependencyInfo(
                        "spring-boot-starter-web", "auto", "compile"));
            }
            if (pomContent.contains("spring-boot-starter-data-jpa")) {
                dependencies.add(new ProjectContext.DependencyInfo(
                        "spring-boot-starter-data-jpa", "auto", "compile"));
            }
            if (pomContent.contains("spring-boot-starter-test")) {
                dependencies.add(new ProjectContext.DependencyInfo(
                        "spring-boot-starter-test", "auto", "test"));
            }
            // 可以添加更多依赖检测逻辑

        } catch (IOException e) {
            logger.warn("Error reading pom.xml", e);
        }

        return dependencies;
    }

    /**
     * 分析NPM依赖
     */
    private List<ProjectContext.DependencyInfo> analyzeNpmDependencies(Path projectRoot) {
        List<ProjectContext.DependencyInfo> dependencies = new ArrayList<>();
        Path packageJsonPath = projectRoot.resolve("package.json");

        if (!Files.exists(packageJsonPath)) {
            return dependencies;
        }

        try {
            String content = Files.readString(packageJsonPath);
            JsonNode packageJson = objectMapper.readTree(content);

            // 分析生产依赖
            JsonNode deps = packageJson.get("dependencies");
            if (deps != null) {
                deps.fields().forEachRemaining(entry -> {
                    dependencies.add(new ProjectContext.DependencyInfo(
                            entry.getKey(), entry.getValue().asText(), "production"));
                });
            }

            // 分析开发依赖
            JsonNode devDeps = packageJson.get("devDependencies");
            if (devDeps != null) {
                devDeps.fields().forEachRemaining(entry -> {
                    ProjectContext.DependencyInfo depInfo = new ProjectContext.DependencyInfo(
                            entry.getKey(), entry.getValue().asText(), "development");
                    depInfo.setDirectDependency(true);
                    dependencies.add(depInfo);
                });
            }

        } catch (IOException e) {
            logger.warn("Error reading package.json", e);
        }

        return dependencies;
    }

    /**
     * 分析Python依赖
     */
    private List<ProjectContext.DependencyInfo> analyzePythonDependencies(Path projectRoot) {
        List<ProjectContext.DependencyInfo> dependencies = new ArrayList<>();
        Path requirementsFile = projectRoot.resolve("requirements.txt");

        if (Files.exists(requirementsFile)) {
            try {
                List<String> lines = Files.readAllLines(requirementsFile);
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        String[] parts = line.split("==|>=|<=|>|<");
                        String name = parts[0].trim();
                        String version = parts.length > 1 ? parts[1].trim() : "latest";
                        dependencies.add(new ProjectContext.DependencyInfo(name, version, "runtime"));
                    }
                }
            } catch (IOException e) {
                logger.warn("Error reading requirements.txt", e);
            }
        }

        return dependencies;
    }

    /**
     * 查找配置文件
     */
    public List<ProjectContext.ConfigFile> findConfigurationFiles(Path projectRoot) {
        logger.debug("Finding configuration files for: {}", projectRoot);

        List<ProjectContext.ConfigFile> configFiles = new ArrayList<>();
        ProjectType projectType = projectTypeDetector.detectProjectType(projectRoot);

        try {
            // 通用配置文件
            addConfigFileIfExists(configFiles, projectRoot, "application.properties", "properties");
            addConfigFileIfExists(configFiles, projectRoot, "application.yml", "yaml");
            addConfigFileIfExists(configFiles, projectRoot, "application.yaml", "yaml");
            addConfigFileIfExists(configFiles, projectRoot, "config.json", "json");

            // 项目类型特定的配置文件
            switch (projectType) {
                case JAVA_MAVEN:
                case SPRING_BOOT:
                    addConfigFileIfExists(configFiles, projectRoot, "pom.xml", "xml");
                    break;
                case NODE_JS:
                case REACT:
                case VUE:
                case ANGULAR:
                case NEXT_JS:
                    addConfigFileIfExists(configFiles, projectRoot, "package.json", "json");
                    addConfigFileIfExists(configFiles, projectRoot, "webpack.config.js", "javascript");
                    break;
                case PYTHON:
                case DJANGO:
                case FLASK:
                case FASTAPI:
                    addConfigFileIfExists(configFiles, projectRoot, "requirements.txt", "text");
                    addConfigFileIfExists(configFiles, projectRoot, "setup.py", "python");
                    break;
            }

        } catch (Exception e) {
            logger.error("Error finding configuration files for: " + projectRoot, e);
        }

        logger.info("Found {} configuration files for project: {}", configFiles.size(), projectRoot);
        return configFiles;
    }

    /**
     * 添加配置文件（如果存在）
     */
    private void addConfigFileIfExists(List<ProjectContext.ConfigFile> configFiles,
                                       Path projectRoot, String fileName, String fileType) {
        Path configPath = projectRoot.resolve(fileName);
        if (Files.exists(configPath)) {
            String relativePath = projectRoot.relativize(configPath).toString();
            ProjectContext.ConfigFile configFile =
                    new ProjectContext.ConfigFile(fileName, relativePath, fileType);

            // 标记主要配置文件
            if (fileName.equals("pom.xml") || fileName.equals("package.json") ||
                    fileName.startsWith("application.")) {
                configFile.setMainConfig(true);
            }

            configFiles.add(configFile);
        }
    }

    /**
     * 检查是否应该忽略目录
     */
    private boolean shouldIgnoreDirectory(String dirName) {
        return dirName.equals(".git") || dirName.equals(".svn") ||
                dirName.equals("node_modules") || dirName.equals("target") ||
                dirName.equals("build") || dirName.equals("dist") ||
                dirName.equals("__pycache__") || dirName.equals(".idea") ||
                dirName.equals(".vscode") || dirName.startsWith(".");
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    /**
     * 检查是否为关键文件
     */
    private boolean isKeyFile(String fileName, ProjectType projectType) {
        // 通用关键文件
        if (fileName.equals("README.md") || fileName.equals("LICENSE") ||
                fileName.equals("Dockerfile") || fileName.equals(".gitignore")) {
            return true;
        }

        // 项目类型特定的关键文件
        if (projectType != null) {
            String keyFile = projectType.getKeyFile();
            if (keyFile != null && !keyFile.isEmpty()) {
                return fileName.equals(keyFile) || fileName.matches(keyFile);
            }
        }

        return false;
    }
}
