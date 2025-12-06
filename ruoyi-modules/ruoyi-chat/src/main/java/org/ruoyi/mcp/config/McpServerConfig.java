package org.ruoyi.mcp.config;

import java.util.List;
import java.util.Map;

public class McpServerConfig {
    private String command;
    private List<String> args;
    private Map<String, String> env;
    private String Description;
    private String workingDirectory;

    // getters and setters
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String toString() {
        return "McpServerConfig{" +
                "command='" + command + '\'' +
                ", args=" + args +
                ", env=" + env +
                ", Description='" + Description + '\'' +
                ", workingDirectory='" + workingDirectory + '\'' +
                '}';
    }
}
