package org.ruoyi.mcp.tools;

import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 编辑文件工具
 * 支持基于diff的文件编辑
 */
@Component
public class EditFileTool implements BuiltinToolProvider {

    public static final String DESCRIPTION = "Edits a file by applying a diff. " +
        "Use this tool when you need to make specific changes to a file. " +
        "The tool will show the diff before applying changes. " +
        "Use absolute paths within the workspace directory.";

    private final String rootDirectory;
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    public EditFileTool() {
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
    }

    /**
     * 编辑文件
     *
     * @param filePath 文件绝对路径
     * @param diff     要应用的diff内容
     * @return 操作结果
     */
    @Tool(DESCRIPTION)
    public String editFile(String filePath, String diff) {
        try {
            // 验证参数
            if (filePath == null || filePath.trim().isEmpty()) {
                return "Error: File path cannot be empty";
            }

            if (diff == null || diff.trim().isEmpty()) {
                return "Error: Diff cannot be empty";
            }

            Path path = Paths.get(filePath);

            // 验证是否为绝对路径
            if (!path.isAbsolute()) {
                return "Error: File path must be absolute: " + filePath;
            }

            // 验证是否在工作目录内
            if (!isWithinWorkspace(path)) {
                return "Error: File path must be within the workspace directory (" + rootDirectory + "): " + filePath;
            }

            // 检查文件是否存在
            if (!Files.exists(path)) {
                return "Error: File not found: " + filePath;
            }

            // 检查是否为目录
            if (Files.isDirectory(path)) {
                return "Error: Path is a directory, not a file: " + filePath;
            }

            // 读取原始内容
            String originalContent = Files.readString(path, StandardCharsets.UTF_8);

            // 应用diff
            try {
                String newContent = applyDiff(originalContent, diff);

                // 写入文件
                Files.writeString(path, newContent, StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

                String relativePath = getRelativePath(path);
                return String.format("Successfully edited file: %s", relativePath);

            } catch (Exception e) {
                return "Error: Failed to apply diff: " + e.getMessage();
            }

        } catch (IOException e) {
            logger.error("Error editing file: {}", filePath, e);
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error editing file: {}", filePath, e);
            return "Error: Unexpected error: " + e.getMessage();
        }
    }

    /**
     * 仅支持 unified diff（包含 @@ hunk 头）
     */
    private String applyDiff(String originalContent, String diff) {
        List<String> originalLines = new ArrayList<>(Arrays.asList(originalContent.split("\n", -1)));
        List<String> diffLines = Arrays.asList(diff.split("\n", -1));

        int i = 0;
        while (i < diffLines.size()) {
            String line = diffLines.get(i);

            if (line.startsWith("---") || line.startsWith("+++")) {
                i++;
                continue;
            }

            if (!line.startsWith("@@")) {
                i++;
                continue;
            }

            HunkHeader header = parseHunkHeader(line);
            int targetIndex = Math.max(0, header.oldStart - 1);
            i++;

            while (i < diffLines.size()) {
                String hunkLine = diffLines.get(i);
                if (hunkLine.startsWith("@@") || hunkLine.startsWith("---") || hunkLine.startsWith("+++")) {
                    break;
                }

                if (hunkLine.startsWith("\\ No newline at end of file")) {
                    i++;
                    continue;
                }

                if (hunkLine.isEmpty()) {
                    // unified diff 中空内容上下文行会表现为空字符串，视为上下文行
                    ensureExpectedLine(originalLines, targetIndex, "");
                    targetIndex++;
                    i++;
                    continue;
                }

                char op = hunkLine.charAt(0);
                String content = hunkLine.length() > 1 ? hunkLine.substring(1) : "";
                switch (op) {
                    case ' ':
                        ensureExpectedLine(originalLines, targetIndex, content);
                        targetIndex++;
                        break;
                    case '-':
                        ensureExpectedLine(originalLines, targetIndex, content);
                        originalLines.remove(targetIndex);
                        break;
                    case '+':
                        originalLines.add(targetIndex, content);
                        targetIndex++;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported diff line: " + hunkLine);
                }
                i++;
            }
        }

        return String.join("\n", originalLines);
    }

    private void ensureExpectedLine(List<String> lines, int index, String expected) {
        if (index < 0 || index >= lines.size()) {
            throw new IllegalArgumentException("Diff out of range at line index: " + index);
        }
        String actual = lines.get(index);
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException("Diff context mismatch at line " + (index + 1)
                + ", expected: [" + expected + "], actual: [" + actual + "]");
        }
    }

    private HunkHeader parseHunkHeader(String headerLine) {
        Pattern p = Pattern.compile("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*");
        Matcher m = p.matcher(headerLine);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid unified diff hunk header: " + headerLine);
        }

        int oldStart = Integer.parseInt(m.group(1));
        int oldCount = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
        int newStart = Integer.parseInt(m.group(3));
        int newCount = m.group(4) == null ? 1 : Integer.parseInt(m.group(4));
        return new HunkHeader(oldStart, oldCount, newStart, newCount);
    }

    private record HunkHeader(int oldStart, int oldCount, int newStart, int newCount) {
    }

    private boolean isWithinWorkspace(Path filePath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
            Path normalizedPath = filePath.normalize();
            return normalizedPath.startsWith(workspaceRoot.normalize());
        } catch (IOException e) {
            logger.warn("Could not resolve workspace path", e);
            return false;
        }
    }

    private String getRelativePath(Path filePath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory);
            return workspaceRoot.relativize(filePath).toString();
        } catch (Exception e) {
            return filePath.toString();
        }
    }

    @Override
    public String getToolName() {
        return "edit_file";
    }

    @Override
    public String getDisplayName() {
        return "编辑文件";
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
