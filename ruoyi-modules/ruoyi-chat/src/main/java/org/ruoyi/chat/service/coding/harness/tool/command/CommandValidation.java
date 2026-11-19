package org.ruoyi.chat.service.coding.harness.tool.command;

import java.util.List;

final class CommandValidation {

    private CommandValidation() {
    }

    static String requireText(String value, String field, int maxChars) {
        if (value == null || value.isBlank() || value.length() > maxChars) {
            throw new CommandToolException("INVALID_COMMAND_ARGUMENT",
                field + " must be non-empty and within its character limit");
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                throw new CommandToolException("INVALID_COMMAND_ARGUMENT",
                    field + " must not contain NUL or control characters");
            }
        }
        return value;
    }

    static List<String> validateArgv(List<String> argv, CommandToolConfig config) {
        if (argv == null) {
            throw new CommandToolException("INVALID_COMMAND_ARGUMENT",
                "argv must be provided as an array; use an empty array for no arguments");
        }
        if (argv.size() > config.maxArgvEntries()) {
            throw new CommandToolException("ARGV_LIMIT_EXCEEDED",
                "argv contains too many entries");
        }
        for (int index = 0; index < argv.size(); index++) {
            requireText(argv.get(index), "argv[" + index + "]", config.maxArgumentChars());
        }
        return List.copyOf(argv);
    }
}
