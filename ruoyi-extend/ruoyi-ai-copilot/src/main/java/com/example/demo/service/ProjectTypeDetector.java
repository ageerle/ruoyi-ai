package com.example.demo.service;

import com.example.demo.model.ProjectType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * 项目类型检测器
 * 基于文件特征自动识别项目类型
 */
@Component
public class ProjectTypeDetector {

    private static final Logger logger = LoggerFactory.getLogger(ProjectTypeDetector.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检测项目类型
     *
     * @param projectRoot 项目根目录
     * @return 检测到的项目类型
     */
    public ProjectType detectProjectType(Path projectRoot) {
        if (!Files.exists(projectRoot) || !Files.isDirectory(projectRoot)) {
            logger.warn("Project root does not exist or is not a directory: {}", projectRoot);
            return ProjectType.UNKNOWN;
        }

        try {
            logger.debug("Detecting project type for: {}", projectRoot);

            // 按优先级检测项目类型
            ProjectType detectedType = detectByKeyFiles(projectRoot);
            if (detectedType != ProjectType.UNKNOWN) {
                logger.info("Detected project type: {} for {}", detectedType, projectRoot);
                return detectedType;
            }

            // 如果关键文件检测失败，尝试基于目录结构检测
            detectedType = detectByDirectoryStructure(projectRoot);
            if (detectedType != ProjectType.UNKNOWN) {
                logger.info("Detected project type by structure: {} for {}", detectedType, projectRoot);
                return detectedType;
            }

            logger.info("Could not determine project type for: {}", projectRoot);
            return ProjectType.UNKNOWN;

        } catch (Exception e) {
            logger.error("Error detecting project type for: " + projectRoot, e);
            return ProjectType.UNKNOWN;
        }
    }

    /**
     * 基于关键文件检测项目类型
     */
    private ProjectType detectByKeyFiles(Path projectRoot) throws IOException {
        // Java Maven项目
        if (Files.exists(projectRoot.resolve("pom.xml"))) {
            // 检查是否为Spring Boot项目
            if (isSpringBootProject(projectRoot)) {
                return ProjectType.SPRING_BOOT;
            }
            return ProjectType.JAVA_MAVEN;
        }

        // Java Gradle项目
        if (Files.exists(projectRoot.resolve("build.gradle")) ||
                Files.exists(projectRoot.resolve("build.gradle.kts"))) {
            return ProjectType.JAVA_GRADLE;
        }

        // Node.js项目
        if (Files.exists(projectRoot.resolve("package.json"))) {
            return analyzeNodeJsProject(projectRoot);
        }

        // Python项目
        if (Files.exists(projectRoot.resolve("requirements.txt")) ||
                Files.exists(projectRoot.resolve("setup.py")) ||
                Files.exists(projectRoot.resolve("pyproject.toml"))) {
            return analyzePythonProject(projectRoot);
        }

        // .NET项目
        try (Stream<Path> files = Files.list(projectRoot)) {
            if (files.anyMatch(path -> path.toString().endsWith(".csproj") ||
                    path.toString().endsWith(".sln"))) {
                return ProjectType.DOTNET;
            }
        }

        // Go项目
        if (Files.exists(projectRoot.resolve("go.mod"))) {
            return ProjectType.GO;
        }

        // Rust项目
        if (Files.exists(projectRoot.resolve("Cargo.toml"))) {
            return ProjectType.RUST;
        }

        // PHP项目
        if (Files.exists(projectRoot.resolve("composer.json"))) {
            return ProjectType.PHP;
        }

        // 静态HTML项目
        if (Files.exists(projectRoot.resolve("index.html"))) {
            return ProjectType.HTML_STATIC;
        }

        return ProjectType.UNKNOWN;
    }

    /**
     * 检查是否为Spring Boot项目
     */
    private boolean isSpringBootProject(Path projectRoot) {
        try {
            Path pomFile = projectRoot.resolve("pom.xml");
            if (!Files.exists(pomFile)) {
                return false;
            }

            String pomContent = Files.readString(pomFile);
            return pomContent.contains("spring-boot-starter") ||
                    pomContent.contains("org.springframework.boot");
        } catch (IOException e) {
            logger.warn("Error reading pom.xml for Spring Boot detection", e);
            return false;
        }
    }

    /**
     * 分析Node.js项目类型
     */
    private ProjectType analyzeNodeJsProject(Path projectRoot) {
        try {
            Path packageJsonPath = projectRoot.resolve("package.json");
            String content = Files.readString(packageJsonPath);
            JsonNode packageJson = objectMapper.readTree(content);

            // 检查依赖来确定具体的框架类型
            JsonNode dependencies = packageJson.get("dependencies");
            JsonNode devDependencies = packageJson.get("devDependencies");

            if (hasDependency(dependencies, "react") || hasDependency(devDependencies, "react")) {
                return ProjectType.REACT;
            }

            if (hasDependency(dependencies, "vue") || hasDependency(devDependencies, "vue")) {
                return ProjectType.VUE;
            }

            if (hasDependency(dependencies, "@angular/core") ||
                    hasDependency(devDependencies, "@angular/cli")) {
                return ProjectType.ANGULAR;
            }

            if (hasDependency(dependencies, "next") || hasDependency(devDependencies, "next")) {
                return ProjectType.NEXT_JS;
            }

            return ProjectType.NODE_JS;

        } catch (IOException e) {
            logger.warn("Error analyzing package.json", e);
            return ProjectType.NODE_JS;
        }
    }

    /**
     * 分析Python项目类型
     */
    private ProjectType analyzePythonProject(Path projectRoot) {
        // 检查Django项目
        if (Files.exists(projectRoot.resolve("manage.py"))) {
            return ProjectType.DJANGO;
        }

        // 检查Flask项目
        if (Files.exists(projectRoot.resolve("app.py")) ||
                Files.exists(projectRoot.resolve("application.py"))) {
            return ProjectType.FLASK;
        }

        // 检查FastAPI项目
        if (Files.exists(projectRoot.resolve("main.py"))) {
            try {
                String content = Files.readString(projectRoot.resolve("main.py"));
                if (content.contains("from fastapi import") || content.contains("import fastapi")) {
                    return ProjectType.FASTAPI;
                }
            } catch (IOException e) {
                logger.warn("Error reading main.py for FastAPI detection", e);
            }
        }

        return ProjectType.PYTHON;
    }

    /**
     * 基于目录结构检测项目类型
     */
    private ProjectType detectByDirectoryStructure(Path projectRoot) {
        try {
            List<String> directories = Files.list(projectRoot)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString().toLowerCase())
                    .toList();

            // Java项目特征目录
            if (directories.contains("src") &&
                    (directories.contains("target") || directories.contains("build"))) {
                return ProjectType.JAVA_MAVEN; // 默认为Maven
            }

            // Node.js项目特征目录
            if (directories.contains("node_modules") ||
                    directories.contains("public") ||
                    directories.contains("dist")) {
                return ProjectType.NODE_JS;
            }

            // Python项目特征目录
            if (directories.contains("venv") ||
                    directories.contains("env") ||
                    directories.contains("__pycache__")) {
                return ProjectType.PYTHON;
            }

        } catch (IOException e) {
            logger.warn("Error analyzing directory structure", e);
        }

        return ProjectType.UNKNOWN;
    }

    /**
     * 检查是否存在特定依赖
     */
    private boolean hasDependency(JsonNode dependencies, String dependencyName) {
        return dependencies != null && dependencies.has(dependencyName);
    }

    /**
     * 获取项目类型的详细信息
     */
    public String getProjectTypeDetails(Path projectRoot, ProjectType projectType) {
        StringBuilder details = new StringBuilder();
        details.append("Project Type: ").append(projectType.getDisplayName()).append("\n");
        details.append("Primary Language: ").append(projectType.getPrimaryLanguage()).append("\n");
        details.append("Package Manager: ").append(projectType.getPackageManager()).append("\n");

        // 添加特定项目类型的详细信息
        switch (projectType) {
            case SPRING_BOOT:
                details.append("Framework: Spring Boot\n");
                details.append("Build Tool: Maven\n");
                break;
            case REACT:
                details.append("Framework: React\n");
                details.append("Runtime: Node.js\n");
                break;
            case DJANGO:
                details.append("Framework: Django\n");
                details.append("Language: Python\n");
                break;
            // 可以添加更多项目类型的详细信息
        }

        return details.toString();
    }
}
