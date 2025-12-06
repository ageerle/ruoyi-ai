package com.example.demo.service;

import com.example.demo.model.ProjectType;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 项目模板扩展服务
 * 生成README、gitignore等通用文件模板
 */
@Service
public class ProjectTemplateExtensions {

    /**
     * 生成README.md内容
     */
    public String generateReadmeContent(Map<String, String> variables) {
        return String.format("""
                        # %s
                                    
                        %s
                                    
                        ## 🚀 Getting Started
                                    
                        ### Prerequisites
                                    
                        - Java 17 or higher (for Java projects)
                        - Node.js 16+ (for JavaScript projects)
                        - Python 3.8+ (for Python projects)
                                    
                        ### Installation
                                    
                        1. Clone the repository:
                        ```bash
                        git clone <repository-url>
                        cd %s
                        ```
                                    
                        2. Install dependencies:
                        ```bash
                        # For Java Maven projects
                        mvn clean install
                                    
                        # For Node.js projects
                        npm install
                                    
                        # For Python projects
                        pip install -r requirements.txt
                        ```
                                    
                        3. Run the application:
                        ```bash
                        # For Java Maven projects
                        mvn spring-boot:run
                                    
                        # For Node.js projects
                        npm start
                                    
                        # For Python projects
                        python main.py
                        ```
                                    
                        ## 📁 Project Structure
                                    
                        ```
                        %s/
                        ├── src/                 # Source code
                        ├── test/               # Test files
                        ├── docs/               # Documentation
                        ├── README.md           # This file
                        └── ...
                        ```
                                    
                        ## 🛠️ Development
                                    
                        ### Running Tests
                                    
                        ```bash
                        # For Java projects
                        mvn test
                                    
                        # For Node.js projects
                        npm test
                                    
                        # For Python projects
                        python -m pytest
                        ```
                                    
                        ### Building
                                    
                        ```bash
                        # For Java projects
                        mvn clean package
                                    
                        # For Node.js projects
                        npm run build
                                    
                        # For Python projects
                        python setup.py build
                        ```
                                    
                        ## 📝 Features
                                    
                        - Feature 1: Description
                        - Feature 2: Description
                        - Feature 3: Description
                                    
                        ## 🤝 Contributing
                                    
                        1. Fork the project
                        2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
                        3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
                        4. Push to the branch (`git push origin feature/AmazingFeature`)
                        5. Open a Pull Request
                                    
                        ## 📄 License
                                    
                        This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
                                    
                        ## 👥 Authors
                                    
                        - **%s** - *Initial work* - [%s](mailto:%s)
                                    
                        ## 🙏 Acknowledgments
                                    
                        - Hat tip to anyone whose code was used
                        - Inspiration
                        - etc
                                    
                        ---
                                    
                        Created with ❤️ by %s
                        """,
                variables.get("PROJECT_NAME_PASCAL"),
                variables.get("DESCRIPTION"),
                variables.get("PROJECT_NAME"),
                variables.get("PROJECT_NAME"),
                variables.get("AUTHOR"),
                variables.get("AUTHOR"),
                variables.get("EMAIL"),
                variables.get("AUTHOR")
        );
    }

    /**
     * 生成.gitignore内容
     */
    public String generateGitignoreContent(ProjectType projectType) {
        StringBuilder gitignore = new StringBuilder();

        // 通用忽略规则
        gitignore.append("""
                # General
                .DS_Store
                .DS_Store?
                ._*
                .Spotlight-V100
                .Trashes
                ehthumbs.db
                Thumbs.db
                            
                # IDE
                .idea/
                .vscode/
                *.swp
                *.swo
                *~
                            
                # Logs
                logs
                *.log
                npm-debug.log*
                yarn-debug.log*
                yarn-error.log*
                            
                # Runtime data
                pids
                *.pid
                *.seed
                *.pid.lock
                            
                # Coverage directory used by tools like istanbul
                coverage/
                            
                # Dependency directories
                node_modules/
                            
                """);

        // 项目类型特定的忽略规则
        switch (projectType) {
            case JAVA_MAVEN:
            case JAVA_GRADLE:
            case SPRING_BOOT:
                gitignore.append("""
                        # Java
                        *.class
                        *.jar
                        *.war
                        *.ear
                        *.nar
                        hs_err_pid*
                                            
                        # Maven
                        target/
                        pom.xml.tag
                        pom.xml.releaseBackup
                        pom.xml.versionsBackup
                        pom.xml.next
                        release.properties
                        dependency-reduced-pom.xml
                        buildNumber.properties
                        .mvn/timing.properties
                        .mvn/wrapper/maven-wrapper.jar
                                            
                        # Gradle
                        .gradle
                        build/
                        !gradle/wrapper/gradle-wrapper.jar
                        !**/src/main/**/build/
                        !**/src/test/**/build/
                                            
                        # Spring Boot
                        spring-boot-*.log
                                            
                        """);
                break;

            case NODE_JS:
            case REACT:
            case VUE:
            case ANGULAR:
            case NEXT_JS:
                gitignore.append("""
                        # Node.js
                        node_modules/
                        npm-debug.log*
                        yarn-debug.log*
                        yarn-error.log*
                        lerna-debug.log*
                                            
                        # Runtime data
                        pids
                        *.pid
                        *.seed
                        *.pid.lock
                                            
                        # Coverage directory used by tools like istanbul
                        coverage/
                        *.lcov
                                            
                        # nyc test coverage
                        .nyc_output
                                            
                        # Grunt intermediate storage
                        .grunt
                                            
                        # Bower dependency directory
                        bower_components
                                            
                        # node-waf configuration
                        .lock-wscript
                                            
                        # Compiled binary addons
                        build/Release
                                            
                        # Dependency directories
                        node_modules/
                        jspm_packages/
                                            
                        # Optional npm cache directory
                        .npm
                                            
                        # Optional eslint cache
                        .eslintcache
                                            
                        # Microbundle cache
                        .rpt2_cache/
                        .rts2_cache_cjs/
                        .rts2_cache_es/
                        .rts2_cache_umd/
                                            
                        # Optional REPL history
                        .node_repl_history
                                            
                        # Output of 'npm pack'
                        *.tgz
                                            
                        # Yarn Integrity file
                        .yarn-integrity
                                            
                        # dotenv environment variables file
                        .env
                        .env.test
                        .env.local
                        .env.development.local
                        .env.test.local
                        .env.production.local
                                            
                        # parcel-bundler cache
                        .cache
                        .parcel-cache
                                            
                        # Next.js build output
                        .next
                        out
                                            
                        # Nuxt.js build / generate output
                        .nuxt
                        dist
                                            
                        # Gatsby files
                        .cache/
                        public
                                            
                        # Storybook build outputs
                        .out
                        .storybook-out
                                            
                        # Temporary folders
                        tmp/
                        temp/
                                            
                        """);
                break;

            case PYTHON:
            case DJANGO:
            case FLASK:
            case FASTAPI:
                gitignore.append("""
                        # Python
                        __pycache__/
                        *.py[cod]
                        *$py.class
                                            
                        # C extensions
                        *.so
                                            
                        # Distribution / packaging
                        .Python
                        build/
                        develop-eggs/
                        dist/
                        downloads/
                        eggs/
                        .eggs/
                        lib/
                        lib64/
                        parts/
                        sdist/
                        var/
                        wheels/
                        *.egg-info/
                        .installed.cfg
                        *.egg
                        MANIFEST
                                            
                        # PyInstaller
                        *.manifest
                        *.spec
                                            
                        # Installer logs
                        pip-log.txt
                        pip-delete-this-directory.txt
                                            
                        # Unit test / coverage reports
                        htmlcov/
                        .tox/
                        .nox/
                        .coverage
                        .coverage.*
                        .cache
                        nosetests.xml
                        coverage.xml
                        *.cover
                        .hypothesis/
                        .pytest_cache/
                                            
                        # Translations
                        *.mo
                        *.pot
                                            
                        # Django stuff:
                        *.log
                        local_settings.py
                        db.sqlite3
                                            
                        # Flask stuff:
                        instance/
                        .webassets-cache
                                            
                        # Scrapy stuff:
                        .scrapy
                                            
                        # Sphinx documentation
                        docs/_build/
                                            
                        # PyBuilder
                        target/
                                            
                        # Jupyter Notebook
                        .ipynb_checkpoints
                                            
                        # IPython
                        profile_default/
                        ipython_config.py
                                            
                        # pyenv
                        .python-version
                                            
                        # celery beat schedule file
                        celerybeat-schedule
                                            
                        # SageMath parsed files
                        *.sage.py
                                            
                        # Environments
                        .env
                        .venv
                        env/
                        venv/
                        ENV/
                        env.bak/
                        venv.bak/
                                            
                        # Spyder project settings
                        .spyderproject
                        .spyproject
                                            
                        # Rope project settings
                        .ropeproject
                                            
                        # mkdocs documentation
                        /site
                                            
                        # mypy
                        .mypy_cache/
                        .dmypy.json
                        dmypy.json
                                            
                        """);
                break;

            default:
                // 基本忽略规则已经添加
                break;
        }

        return gitignore.toString();
    }
}
