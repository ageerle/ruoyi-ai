package com.example.demo.service;

import com.example.demo.model.ProjectType;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 项目模板服务
 * 生成各种项目类型的模板文件内容
 */
@Service
public class ProjectTemplateService {
    
    /**
     * 生成Maven pom.xml
     */
    public String generatePomXml(Map<String, String> variables) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <packaging>jar</packaging>
                
                <name>%s</name>
                <description>%s</description>
                
                <properties>
                    <maven.compiler.source>17</maven.compiler.source>
                    <maven.compiler.target>17</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>
                
                <dependencies>
                    <dependency>
                        <groupId>junit</groupId>
                        <artifactId>junit</artifactId>
                        <version>4.13.2</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.11.0</version>
                            <configuration>
                                <source>17</source>
                                <target>17</target>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """,
            variables.get("PROJECT_NAME"),
            variables.get("VERSION"),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("DESCRIPTION")
        );
    }
    
    /**
     * 生成Spring Boot pom.xml
     */
    public String generateSpringBootPomXml(Map<String, String> variables) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>3.2.1</version>
                    <relativePath/>
                </parent>
                
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <packaging>jar</packaging>
                
                <name>%s</name>
                <description>%s</description>
                
                <properties>
                    <java.version>17</java.version>
                </properties>
                
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                    </dependency>
                    
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-test</artifactId>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
                
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-maven-plugin</artifactId>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """,
            variables.get("PROJECT_NAME"),
            variables.get("VERSION"),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("DESCRIPTION")
        );
    }
    
    /**
     * 生成Java主类
     */
    public String generateJavaMainClass(Map<String, String> variables) {
        return String.format("""
            package com.example.%s;
            
            /**
             * Main application class for %s
             * 
             * @author %s
             */
            public class Application {
                
                public static void main(String[] args) {
                    System.out.println("Hello from %s!");
                    System.out.println("Application started successfully.");
                }
                
                /**
                 * Get application name
                 * @return application name
                 */
                public String getApplicationName() {
                    return "%s";
                }
            }
            """,
            variables.get("PROJECT_NAME").toLowerCase(),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("AUTHOR"),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("PROJECT_NAME_PASCAL")
        );
    }
    
    /**
     * 生成Spring Boot主类
     */
    public String generateSpringBootMainClass(Map<String, String> variables) {
        return String.format("""
            package com.example.%s;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            
            /**
             * Spring Boot main application class for %s
             * 
             * @author %s
             */
            @SpringBootApplication
            public class Application {
                
                public static void main(String[] args) {
                    SpringApplication.run(Application.class, args);
                }
            }
            """,
            variables.get("PROJECT_NAME").toLowerCase(),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("AUTHOR")
        );
    }
    
    /**
     * 生成Spring Boot Controller
     */
    public String generateSpringBootController(Map<String, String> variables) {
        return String.format("""
            package com.example.%s.controller;
            
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;
            
            /**
             * Hello controller for %s
             * 
             * @author %s
             */
            @RestController
            public class HelloController {
                
                @GetMapping("/")
                public String hello() {
                    return "Hello from %s!";
                }
                
                @GetMapping("/health")
                public String health() {
                    return "OK";
                }
            }
            """,
            variables.get("PROJECT_NAME").toLowerCase(),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("AUTHOR"),
            variables.get("PROJECT_NAME_PASCAL")
        );
    }
    
    /**
     * 生成Java测试类
     */
    public String generateJavaTestClass(Map<String, String> variables) {
        return String.format("""
            package com.example.%s;
            
            import org.junit.Test;
            import static org.junit.Assert.*;
            
            /**
             * Test class for %s Application
             * 
             * @author %s
             */
            public class ApplicationTest {
                
                @Test
                public void testApplicationName() {
                    Application app = new Application();
                    assertEquals("%s", app.getApplicationName());
                }
                
                @Test
                public void testApplicationCreation() {
                    Application app = new Application();
                    assertNotNull(app);
                }
            }
            """,
            variables.get("PROJECT_NAME").toLowerCase(),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("AUTHOR"),
            variables.get("PROJECT_NAME_PASCAL")
        );
    }
    
    /**
     * 生成application.yml
     */
    public String generateApplicationYml(Map<String, String> variables) {
        return String.format("""
            # Application configuration for %s
            server:
              port: 8080
              servlet:
                context-path: /
            
            spring:
              application:
                name: %s
              profiles:
                active: dev
            
            # Logging configuration
            logging:
              level:
                com.example.%s: DEBUG
                org.springframework: INFO
              pattern:
                console: "%%d{yyyy-MM-dd HH:mm:ss} - %%msg%%n"
            
            # Management endpoints
            management:
              endpoints:
                web:
                  exposure:
                    include: health,info
              endpoint:
                health:
                  show-details: when-authorized
            """,
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("PROJECT_NAME"),
            variables.get("PROJECT_NAME").toLowerCase()
        );
    }
    
    /**
     * 生成package.json
     */
    public String generatePackageJson(Map<String, String> variables) {
        return String.format("""
            {
              "name": "%s",
              "version": "%s",
              "description": "%s",
              "main": "index.js",
              "scripts": {
                "start": "node index.js",
                "test": "echo \\"Error: no test specified\\" && exit 1",
                "dev": "node index.js"
              },
              "keywords": [
                "nodejs",
                "%s"
              ],
              "author": "%s <%s>",
              "license": "MIT",
              "dependencies": {},
              "devDependencies": {}
            }
            """,
            variables.get("PROJECT_NAME"),
            variables.get("VERSION"),
            variables.get("DESCRIPTION"),
            variables.get("PROJECT_NAME"),
            variables.get("AUTHOR"),
            variables.get("EMAIL")
        );
    }
    
    /**
     * 生成Node.js主文件
     */
    public String generateNodeJsMainFile(Map<String, String> variables) {
        return String.format("""
            /**
             * Main application file for %s
             * 
             * @author %s
             */
            
            console.log('Hello from %s!');
            console.log('Node.js application started successfully.');
            
            // Simple HTTP server example
            const http = require('http');
            
            const server = http.createServer((req, res) => {
                res.writeHead(200, { 'Content-Type': 'text/plain' });
                res.end('Hello from %s!\\n');
            });
            
            const PORT = process.env.PORT || 3000;
            server.listen(PORT, () => {
                console.log(`Server running on port ${PORT}`);
            });
            """,
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("AUTHOR"),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("PROJECT_NAME_PASCAL")
        );
    }

    /**
     * 生成React App.js
     */
    public String generateReactAppJs(Map<String, String> variables) {
        return String.format("""
            import React from 'react';
            import './App.css';

            /**
             * Main App component for %s
             *
             * @author %s
             */
            function App() {
              return (
                <div className="App">
                  <header className="App-header">
                    <h1>Welcome to %s</h1>
                    <p>%s</p>
                    <p>
                      Edit <code>src/App.js</code> and save to reload.
                    </p>
                  </header>
                </div>
              );
            }

            export default App;
            """,
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("AUTHOR"),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("DESCRIPTION")
        );
    }

    /**
     * 生成React index.html
     */
    public String generateReactIndexHtml(Map<String, String> variables) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <meta name="description" content="%s">
                <meta name="author" content="%s">
            </head>
            <body>
                <noscript>You need to enable JavaScript to run this app.</noscript>
                <div id="root"></div>
            </body>
            </html>
            """,
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("DESCRIPTION"),
            variables.get("AUTHOR")
        );
    }

    /**
     * 生成Python主文件
     */
    public String generatePythonMainFile(Map<String, String> variables) {
        return String.format("""
            #!/usr/bin/env python3
            \"\"\"
            Main application file for %s

            Author: %s
            \"\"\"

            def main():
                \"\"\"Main function\"\"\"
                print("Hello from %s!")
                print("Python application started successfully.")

            def get_application_name():
                \"\"\"Get application name\"\"\"
                return "%s"

            if __name__ == "__main__":
                main()
            """,
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("AUTHOR"),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("PROJECT_NAME_PASCAL")
        );
    }

    /**
     * 生成requirements.txt
     */
    public String generateRequirementsTxt(Map<String, String> variables) {
        return """
            # Python dependencies for """ + variables.get("PROJECT_NAME") + """
            # Add your dependencies here

            # Example dependencies:
            # requests>=2.28.0
            # flask>=2.3.0
            # pytest>=7.0.0
            """;
    }

    /**
     * 生成静态HTML index.html
     */
    public String generateStaticIndexHtml(Map<String, String> variables) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <meta name="description" content="%s">
                <meta name="author" content="%s">
                <link rel="stylesheet" href="css/style.css">
            </head>
            <body>
                <header>
                    <h1>Welcome to %s</h1>
                </header>

                <main>
                    <section>
                        <h2>About</h2>
                        <p>%s</p>
                    </section>

                    <section>
                        <h2>Features</h2>
                        <ul>
                            <li>Modern HTML5 structure</li>
                            <li>Responsive design</li>
                            <li>Clean CSS styling</li>
                            <li>JavaScript functionality</li>
                        </ul>
                    </section>
                </main>

                <footer>
                    <p>&copy; %s %s. All rights reserved.</p>
                </footer>

                <script src="js/script.js"></script>
            </body>
            </html>
            """,
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("DESCRIPTION"),
            variables.get("AUTHOR"),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("DESCRIPTION"),
            variables.get("CURRENT_YEAR"),
            variables.get("AUTHOR")
        );
    }

    /**
     * 生成基本CSS
     */
    public String generateBasicCss(Map<String, String> variables) {
        return String.format("""
            /* CSS styles for %s */

            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                line-height: 1.6;
                color: #333;
                background-color: #f4f4f4;
            }

            header {
                background: #35424a;
                color: white;
                padding: 1rem 0;
                text-align: center;
            }

            header h1 {
                margin: 0;
            }

            main {
                max-width: 800px;
                margin: 2rem auto;
                padding: 0 1rem;
                background: white;
                border-radius: 8px;
                box-shadow: 0 2px 5px rgba(0,0,0,0.1);
            }

            section {
                padding: 2rem;
            }

            h2 {
                color: #35424a;
                margin-bottom: 1rem;
            }

            ul {
                margin-left: 2rem;
            }

            li {
                margin-bottom: 0.5rem;
            }

            footer {
                text-align: center;
                padding: 1rem;
                background: #35424a;
                color: white;
                margin-top: 2rem;
            }

            @media (max-width: 768px) {
                main {
                    margin: 1rem;
                }

                section {
                    padding: 1rem;
                }
            }
            """,
            variables.get("PROJECT_NAME_PASCAL")
        );
    }

    /**
     * 生成基本JavaScript
     */
    public String generateBasicJs(Map<String, String> variables) {
        return String.format("""
            /**
             * JavaScript functionality for %s
             *
             * @author %s
             */

            // Wait for DOM to be fully loaded
            document.addEventListener('DOMContentLoaded', function() {
                console.log('%s application loaded successfully!');

                // Add click event to header
                const header = document.querySelector('header h1');
                if (header) {
                    header.addEventListener('click', function() {
                        alert('Welcome to %s!');
                    });
                }

                // Add smooth scrolling for anchor links
                const links = document.querySelectorAll('a[href^="#"]');
                links.forEach(link => {
                    link.addEventListener('click', function(e) {
                        e.preventDefault();
                        const target = document.querySelector(this.getAttribute('href'));
                        if (target) {
                            target.scrollIntoView({
                                behavior: 'smooth'
                            });
                        }
                    });
                });
            });

            /**
             * Utility function to get application name
             */
            function getApplicationName() {
                return '%s';
            }

            /**
             * Utility function to show notification
             */
            function showNotification(message) {
                console.log('Notification:', message);
                // You can implement a proper notification system here
            }
            """,
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("AUTHOR"),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("PROJECT_NAME_PASCAL"),
            variables.get("PROJECT_NAME_PASCAL")
        );
    }
}
