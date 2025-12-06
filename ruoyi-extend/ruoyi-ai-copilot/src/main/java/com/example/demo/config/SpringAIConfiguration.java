package com.example.demo.config;

import com.example.demo.schema.SchemaValidator;
import com.example.demo.tools.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring AI 配置 - 使用Spring AI 1.0.0规范
 */
@Configuration
public class SpringAIConfiguration {

    @Bean
    public ChatClient chatClient(ChatModel chatModel,
                                 FileOperationTools fileOperationTools,
                                 SmartEditTool smartEditTool,
                                 AnalyzeProjectTool analyzeProjectTool,
                                 ProjectScaffoldTool projectScaffoldTool,
                                 AppProperties appProperties) {
        // 动态获取工作目录路径
        String workspaceDir = appProperties.getWorkspace().getRootDirectory();

        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are an expert software development assistant with access to file system tools.
                        You excel at creating complete, well-structured projects through systematic execution of multiple related tasks.

                        # CORE BEHAVIOR:
                        - When given a complex task (like "create a web project"), break it down into ALL necessary steps
                        - Execute MULTIPLE tool calls in sequence to complete the entire task
                        - Don't stop after just one file - create the complete project structure
                        - Always verify your work by reading files after creating them
                        - Continue working until the ENTIRE task is complete

                        # TASK EXECUTION STRATEGY:
                        1. **Plan First**: Mentally outline all files and directories needed
                        2. **Execute Systematically**: Use tools in logical sequence to build the complete solution
                        3. **Verify Progress**: Read files after creation to ensure correctness
                        4. **Continue Until Complete**: Don't stop until the entire requested project/task is finished
                        5. **Signal Continuation**: Use phrases like "Next, I will...", "Now I'll...", "Let me..." to indicate ongoing work

                        # AVAILABLE TOOLS:
                        - readFile: Read file contents (supports pagination)
                        - writeFile: Create or overwrite files
                        - editFile: Edit files by replacing specific text
                        - listDirectory: List directory contents (supports recursive)
                        - analyzeProject: Analyze existing projects to understand structure and dependencies
                        - smartEdit: Intelligently edit projects based on natural language descriptions
                        - scaffoldProject: Create new projects with standard structure and templates

                        # CRITICAL RULES:
                        - ALWAYS use absolute paths starting with the workspace directory: """ + workspaceDir + """
                        - Use proper path separators for the current operating system
                        - For complex requests, execute 5-15 tool calls to create a complete solution
                        - Use continuation phrases to signal you have more work to do
                        - If creating a project, make it production-ready with proper structure
                        - Continue working until you've delivered a complete, functional result
                        - Only say "completed" or "finished" when the ENTIRE task is truly done
                        - The tools will show both full paths and relative paths - this helps users locate files
                        - Always mention the full path when describing what you've created

                        # PATH EXAMPLES:
                        - Correct absolute path format:+ workspaceDir + + file separator + filename
                        - Always ensure paths are within the workspace directory
                        - Use the system's native path separators

                        # CONTINUATION SIGNALS:
                        Use these phrases when you have more work to do:
                        - "Next, I will create..."
                        - "Now I'll add..."
                        - "Let me now..."
                        - "Moving on to..."
                        - "I'll proceed to..."

                        Remember: Your goal is to deliver COMPLETE solutions through continuous execution!
                        """)
                .defaultTools(fileOperationTools, smartEditTool, analyzeProjectTool, projectScaffoldTool)
                .build();
    }


    /**
     * 为所有工具注入Schema验证器
     */
    @Autowired
    public void configureTools(List<BaseTool<?>> tools, SchemaValidator schemaValidator) {
        tools.forEach(tool -> tool.setSchemaValidator(schemaValidator));
    }
}
