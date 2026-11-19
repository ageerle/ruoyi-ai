package org.ruoyi.chat.service.coding.harness.tool.builtin;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Resolves every path against a canonical lease root and rejects link/reparse traversal. */
final class WorkspaceLeaseGuard {

    private final Path root;

    WorkspaceLeaseGuard(RunContext context) {
        this.root = context.leaseRoot();
    }

    Path root() {
        return root;
    }

    Path existingFile(String input) {
        Path path = existing(input);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new BuiltinToolException("NOT_A_FILE", "Path is not a regular file: " + relative(path));
        }
        return path;
    }

    Path existingDirectory(String input) {
        Path path = existing(input == null || input.isBlank() ? "." : input);
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new BuiltinToolException("NOT_A_DIRECTORY", "Path is not a directory: " + relative(path));
        }
        return path;
    }

    Path prepareWriteTarget(String input) {
        Path target = writeTarget(input);
        Path parent = target.getParent();
        ensureDirectories(parent);
        verifyUnlinkedChain(parent);
        Path realParent = real(parent);
        Path guarded = realParent.resolve(target.getFileName().toString()).normalize();
        if (!guarded.startsWith(root)) {
            throw outside(input);
        }
        return guarded;
    }

    Path writeTarget(String input) {
        Path target = lexical(input);
        if (target.equals(root) || target.getFileName() == null) {
            throw new BuiltinToolException("INVALID_PATH", "A file path inside the workspace is required");
        }
        Path parent = target.getParent();
        Path existingAncestor = parent;
        while (existingAncestor != null && !Files.exists(existingAncestor, LinkOption.NOFOLLOW_LINKS)) {
            existingAncestor = existingAncestor.getParent();
        }
        if (existingAncestor == null) {
            throw new BuiltinToolException("IO_ERROR", "Write target has no existing workspace ancestor");
        }
        verifyUnlinkedChain(existingAncestor);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            verifyUnlinkedChain(target);
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new BuiltinToolException("NOT_A_FILE", "Path is not a regular file: " + relative(target));
            }
        }
        return target;
    }

    String relative(Path path) {
        return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    boolean isUnlinkedDirectory(Path path) {
        try {
            return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(path)
                && real(path).equals(path.toAbsolutePath().normalize())
                && real(path).startsWith(root);
        } catch (BuiltinToolException error) {
            return false;
        }
    }

    boolean isUnlinkedRegularFile(Path path) {
        try {
            return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(path)
                && real(path).equals(path.toAbsolutePath().normalize())
                && real(path).startsWith(root);
        } catch (BuiltinToolException error) {
            return false;
        }
    }

    private Path existing(String input) {
        Path path = lexical(input);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new BuiltinToolException("NOT_FOUND", "Path does not exist: " + relative(path));
        }
        verifyUnlinkedChain(path);
        return path;
    }

    private Path lexical(String input) {
        if (input == null || input.isBlank()) {
            throw new BuiltinToolException("INVALID_PATH", "Path is required");
        }
        final Path supplied;
        try {
            supplied = Path.of(input);
        } catch (RuntimeException error) {
            throw new BuiltinToolException("INVALID_PATH", "Invalid path: " + input, error);
        }
        Path path = supplied.isAbsolute()
            ? supplied.toAbsolutePath().normalize()
            : root.resolve(supplied).normalize();
        if (!path.startsWith(root)) {
            throw outside(input);
        }
        return path;
    }

    private void ensureDirectories(Path parent) {
        Path relativeParent = root.relativize(parent);
        Path current = root;
        for (Path segment : relativeParent) {
            current = current.resolve(segment);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    Files.createDirectory(current);
                } catch (FileAlreadyExistsException ignored) {
                    // A concurrent creator won; the checks below validate what it created.
                } catch (IOException error) {
                    throw io("Cannot create parent directory: " + relative(current), error);
                }
            }
            verifyUnlinkedChain(current);
            if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw new BuiltinToolException("NOT_A_DIRECTORY",
                    "Write parent is not a directory: " + relative(current));
            }
        }
    }

    private void verifyUnlinkedChain(Path target) {
        Path relative = root.relativize(target.toAbsolutePath().normalize());
        Path current = root;
        for (Path segment : relative) {
            current = current.resolve(segment);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                break;
            }
            Path normalized = current.toAbsolutePath().normalize();
            Path resolved = real(current);
            if (Files.isSymbolicLink(current) || !resolved.equals(normalized) || !resolved.startsWith(root)) {
                throw new BuiltinToolException("LINK_TRAVERSAL_DENIED",
                    "Symbolic link or reparse traversal is not allowed: " + relative(current));
            }
        }
    }

    private Path real(Path path) {
        try {
            return path.toRealPath().normalize();
        } catch (IOException error) {
            throw io("Cannot resolve workspace path: " + path, error);
        }
    }

    private BuiltinToolException outside(String input) {
        return new BuiltinToolException("OUTSIDE_WORKSPACE",
            "Path is outside the workspace lease: " + input);
    }

    private BuiltinToolException io(String message, IOException error) {
        return new BuiltinToolException("IO_ERROR", message, error);
    }
}
