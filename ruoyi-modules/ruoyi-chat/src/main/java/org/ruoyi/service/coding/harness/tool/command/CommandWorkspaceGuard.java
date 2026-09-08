package org.ruoyi.service.coding.harness.tool.command;

import org.ruoyi.service.coding.harness.tool.builtin.RunContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/** Resolves an existing relative cwd without following a link or Windows reparse/junction node. */
final class CommandWorkspaceGuard {

    private final Path root;
    private final Object rootFileKey;
    private final FileTime rootCreationTime;

    CommandWorkspaceGuard(RunContext context) {
        this.root = context.leaseRoot();
        BasicFileAttributes attributes = readRootAttributes();
        this.rootFileKey = attributes.fileKey();
        this.rootCreationTime = attributes.creationTime();
        verifyRootIdentity();
    }

    Path cwd(String input) {
        verifyRootIdentity();
        if (input == null || input.isBlank() || input.equals(".")) {
            return root;
        }
        CommandValidation.requireText(input, "cwd", 4_096);
        final Path relative;
        try {
            relative = Path.of(input);
        } catch (InvalidPathException error) {
            throw new CommandToolException("INVALID_CWD", "cwd is not a valid relative path", error);
        }
        if (relative.isAbsolute()) {
            throw new CommandToolException("CWD_OUTSIDE_WORKSPACE",
                "cwd must be workspace-relative");
        }
        for (Path segment : relative) {
            if (segment.toString().equals("..")) {
                throw new CommandToolException("CWD_OUTSIDE_WORKSPACE",
                    "cwd must not contain parent traversal");
            }
        }
        Path lexical = root.resolve(relative).normalize();
        if (!lexical.startsWith(root)) {
            throw new CommandToolException("CWD_OUTSIDE_WORKSPACE",
                "cwd escapes the immutable workspace lease");
        }
        verifyChain(lexical);
        try {
            Path real = lexical.toRealPath();
            if (!real.startsWith(root) || !real.equals(lexical.toAbsolutePath().normalize())) {
                throw new CommandToolException("CWD_LINK_TRAVERSAL_DENIED",
                    "cwd resolves through a link or outside the workspace lease");
            }
            return real;
        } catch (IOException error) {
            throw new CommandToolException("INVALID_CWD", "cwd cannot be resolved", error);
        }
    }

    private BasicFileAttributes readRootAttributes() {
        try {
            BasicFileAttributes attributes = Files.readAttributes(root,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory() || attributes.isSymbolicLink() || attributes.isOther()
                || Files.isSymbolicLink(root)) {
                throw new CommandToolException("WORKSPACE_IDENTITY_CHANGED",
                    "Workspace lease root is no longer an ordinary directory");
            }
            return attributes;
        } catch (IOException error) {
            throw new CommandToolException("WORKSPACE_IDENTITY_CHANGED",
                "Workspace lease root identity cannot be verified", error);
        }
    }

    private void verifyRootIdentity() {
        BasicFileAttributes current = readRootAttributes();
        boolean fileKeyChanged = rootFileKey != null && current.fileKey() != null
            && !rootFileKey.equals(current.fileKey());
        boolean fallbackIdentityChanged = (rootFileKey == null || current.fileKey() == null)
            && !rootCreationTime.equals(current.creationTime());
        try {
            Path real = root.toRealPath();
            if (fileKeyChanged || fallbackIdentityChanged
                || !real.equals(root.toAbsolutePath().normalize())) {
                throw new CommandToolException("WORKSPACE_IDENTITY_CHANGED",
                    "Workspace lease root was replaced after authorization");
            }
        } catch (IOException error) {
            throw new CommandToolException("WORKSPACE_IDENTITY_CHANGED",
                "Workspace lease root identity cannot be resolved", error);
        }
    }

    private void verifyChain(Path target) {
        Path current = root;
        for (Path segment : root.relativize(target)) {
            current = current.resolve(segment);
            BasicFileAttributes attributes;
            try {
                attributes = Files.readAttributes(current, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            } catch (IOException error) {
                throw new CommandToolException("INVALID_CWD",
                    "cwd must be an existing workspace directory", error);
            }
            if (!attributes.isDirectory() || attributes.isSymbolicLink() || attributes.isOther()
                || Files.isSymbolicLink(current)) {
                throw new CommandToolException("CWD_LINK_TRAVERSAL_DENIED",
                    "cwd contains a link, junction, reparse point, or non-directory node");
            }
            try {
                Path real = current.toRealPath();
                if (!real.startsWith(root)
                    || !real.equals(current.toAbsolutePath().normalize())) {
                    throw new CommandToolException("CWD_LINK_TRAVERSAL_DENIED",
                        "cwd resolves through a link or outside the workspace lease");
                }
            } catch (IOException error) {
                throw new CommandToolException("INVALID_CWD", "cwd cannot be resolved", error);
            }
        }
    }
}
