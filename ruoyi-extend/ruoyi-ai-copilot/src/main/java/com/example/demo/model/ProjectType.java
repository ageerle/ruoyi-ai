package com.example.demo.model;

/**
 * Project type enumeration
 * Supports mainstream project type detection
 */
public enum ProjectType {
    // Java projects
    JAVA_MAVEN("Java Maven", "pom.xml", "Maven-based Java project"),
    JAVA_GRADLE("Java Gradle", "build.gradle", "Gradle-based Java project"),
    SPRING_BOOT("Spring Boot", "pom.xml", "Spring Boot application"),

    // JavaScript/Node.js projects
    NODE_JS("Node.js", "package.json", "Node.js project"),
    REACT("React", "package.json", "React application"),
    VUE("Vue.js", "package.json", "Vue.js application"),
    ANGULAR("Angular", "package.json", "Angular application"),
    NEXT_JS("Next.js", "package.json", "Next.js application"),

    // Python projects
    PYTHON("Python", "requirements.txt", "Python project"),
    DJANGO("Django", "manage.py", "Django web application"),
    FLASK("Flask", "app.py", "Flask web application"),
    FASTAPI("FastAPI", "main.py", "FastAPI application"),

    // Other project types
    DOTNET("ASP.NET", "*.csproj", ".NET project"),
    GO("Go", "go.mod", "Go project"),
    RUST("Rust", "Cargo.toml", "Rust project"),
    PHP("PHP", "composer.json", "PHP project"),

    // Web frontend
    HTML_STATIC("Static HTML", "index.html", "Static HTML website"),

    // Unknown type
    UNKNOWN("Unknown", "", "Unknown project type");
    
    private final String displayName;
    private final String keyFile;
    private final String description;
    
    ProjectType(String displayName, String keyFile, String description) {
        this.displayName = displayName;
        this.keyFile = keyFile;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getKeyFile() {
        return keyFile;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if it's a Java project
     */
    public boolean isJavaProject() {
        return this == JAVA_MAVEN || this == JAVA_GRADLE || this == SPRING_BOOT;
    }
    
    /**
     * Check if it's a JavaScript project
     */
    public boolean isJavaScriptProject() {
        return this == NODE_JS || this == REACT || this == VUE || 
               this == ANGULAR || this == NEXT_JS;
    }
    
    /**
     * Check if it's a Python project
     */
    public boolean isPythonProject() {
        return this == PYTHON || this == DJANGO || this == FLASK || this == FASTAPI;
    }
    
    /**
     * Check if it's a Web project
     */
    public boolean isWebProject() {
        return isJavaScriptProject() || this == HTML_STATIC || 
               this == DJANGO || this == FLASK || this == FASTAPI || this == SPRING_BOOT;
    }
    
    /**
     * Get the primary programming language of the project
     */
    public String getPrimaryLanguage() {
        if (isJavaProject()) return "Java";
        if (isJavaScriptProject()) return "JavaScript";
        if (isPythonProject()) return "Python";
        
        switch (this) {
            case DOTNET: return "C#";
            case GO: return "Go";
            case RUST: return "Rust";
            case PHP: return "PHP";
            case HTML_STATIC: return "HTML";
            default: return "Unknown";
        }
    }
    
    /**
     * Get the recommended package manager
     */
    public String getPackageManager() {
        switch (this) {
            case JAVA_MAVEN:
            case SPRING_BOOT:
                return "Maven";
            case JAVA_GRADLE:
                return "Gradle";
            case NODE_JS:
            case REACT:
            case VUE:
            case ANGULAR:
            case NEXT_JS:
                return "npm/yarn";
            case PYTHON:
            case DJANGO:
            case FLASK:
            case FASTAPI:
                return "pip";
            case DOTNET:
                return "NuGet";
            case GO:
                return "go mod";
            case RUST:
                return "Cargo";
            case PHP:
                return "Composer";
            default:
                return "Unknown";
        }
    }
}
