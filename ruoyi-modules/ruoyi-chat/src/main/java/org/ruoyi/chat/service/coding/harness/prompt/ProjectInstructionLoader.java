package org.ruoyi.chat.service.coding.harness.prompt;

import org.ruoyi.chat.service.coding.WorkspaceGuard;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

/** Loads one precedence-selected project instruction file without leaving the workspace lease. */
@Service
public class ProjectInstructionLoader {

    private static final long MAX_INSTRUCTION_BYTES = 128 * 1024;
    private static final List<String> PRECEDENCE = List.of(
        "AGENTS.override.md", "AGENTS.md", "CLAUDE.md", ".github/copilot-instructions.md");

    public String load(Path workspace) {
        Path root;
        try {
            root = workspace.toRealPath();
        } catch (IOException error) {
            throw new IllegalArgumentException("Cannot resolve workspace instructions root", error);
        }
        for (String relative : PRECEDENCE) {
            Path candidate = root.resolve(relative).normalize();
            if (!WorkspaceGuard.isWithinWorkspace(root, candidate)
                || !Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            try {
                long size = Files.size(candidate);
                if (size > MAX_INSTRUCTION_BYTES) {
                    return "Instruction file " + relative + " was skipped because it exceeds "
                        + MAX_INSTRUCTION_BYTES + " bytes.";
                }
                return "Source: " + relative + "\n\n"
                    + Files.readString(candidate, StandardCharsets.UTF_8);
            } catch (IOException error) {
                throw new IllegalArgumentException("Cannot read project instruction " + relative, error);
            }
        }
        return "";
    }
}
