package org.ruoyi.chat.service.coding.harness.tool.builtin;

import java.util.regex.Pattern;

/** Platform-neutral git-style glob matcher over slash-normalized relative paths. */
final class PathGlob {

    private final Pattern pattern;
    private final boolean basenameOnly;

    private PathGlob(Pattern pattern, boolean basenameOnly) {
        this.pattern = pattern;
        this.basenameOnly = basenameOnly;
    }

    static PathGlob compile(String expression) {
        return compile(expression, false);
    }

    static PathGlob compile(String expression, boolean forceFullPath) {
        if (expression == null || expression.isBlank()) {
            expression = "**/*";
        }
        String glob = expression.replace('\\', '/');
        while (glob.startsWith("./")) {
            glob = glob.substring(2);
        }
        if (glob.startsWith("/")) {
            glob = glob.substring(1);
        }
        boolean basenameOnly = !forceFullPath && !glob.contains("/");
        return new PathGlob(Pattern.compile(toRegex(glob)), basenameOnly);
    }

    boolean matches(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        if (basenameOnly) {
            int slash = normalized.lastIndexOf('/');
            normalized = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        }
        return pattern.matcher(normalized).matches();
    }

    private static String toRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (int index = 0; index < glob.length(); index++) {
            char current = glob.charAt(index);
            if (current == '*') {
                boolean doubleStar = index + 1 < glob.length() && glob.charAt(index + 1) == '*';
                if (doubleStar) {
                    index++;
                    if (index + 1 < glob.length() && glob.charAt(index + 1) == '/') {
                        index++;
                        regex.append("(?:.*/)?");
                    } else {
                        regex.append(".*");
                    }
                } else {
                    regex.append("[^/]*");
                }
            } else if (current == '?') {
                regex.append("[^/]");
            } else if (current == '[') {
                int closing = glob.indexOf(']', index + 1);
                if (closing > index + 1) {
                    String group = glob.substring(index + 1, closing);
                    if (group.startsWith("!")) {
                        group = "^" + group.substring(1);
                    }
                    regex.append('[').append(group).append(']');
                    index = closing;
                } else {
                    regex.append("\\[");
                }
            } else {
                if (".(){}+$^|\\".indexOf(current) >= 0) {
                    regex.append('\\');
                }
                regex.append(current);
            }
        }
        return regex.append('$').toString();
    }
}
