package com.example.demo.config;

/**
 * 任务上下文持有者
 * 使用ThreadLocal存储当前任务ID，供AOP切面使用
 */
public class TaskContextHolder {

    private static final ThreadLocal<String> taskIdHolder = new ThreadLocal<>();

    /**
     * 获取当前任务ID
     */
    public static String getCurrentTaskId() {
        return taskIdHolder.get();
    }

    /**
     * 设置当前任务ID
     */
    public static void setCurrentTaskId(String taskId) {
        taskIdHolder.set(taskId);
    }

    /**
     * 清除当前任务ID
     */
    public static void clearCurrentTaskId() {
        taskIdHolder.remove();
    }

    /**
     * 检查是否有当前任务ID
     */
    public static boolean hasCurrentTaskId() {
        return taskIdHolder.get() != null;
    }
}
