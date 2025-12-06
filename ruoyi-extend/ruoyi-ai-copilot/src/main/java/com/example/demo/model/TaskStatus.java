package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

public class TaskStatus {
    private String taskId;
    private String status; // RUNNING, COMPLETED, ERROR
    private String currentAction;
    private String summary;
    private int currentTurn;
    private int totalEstimatedTurns;
    private long startTime;
    private long lastUpdateTime;
    private List<String> actionHistory;
    private String errorMessage;
    private double progressPercentage;

    public TaskStatus(String taskId) {
        this.taskId = taskId;
        this.status = "RUNNING";
        this.startTime = System.currentTimeMillis();
        this.lastUpdateTime = this.startTime;
        this.actionHistory = new ArrayList<>();
        this.progressPercentage = 0.0;
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public String getCurrentAction() {
        return currentAction;
    }

    public void setCurrentAction(String currentAction) {
        this.currentAction = currentAction;
        this.lastUpdateTime = System.currentTimeMillis();
        if (currentAction != null && !currentAction.trim().isEmpty()) {
            this.actionHistory.add(currentAction);
        }
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(int currentTurn) {
        this.currentTurn = currentTurn;
        updateProgress();
    }

    public int getTotalEstimatedTurns() {
        return totalEstimatedTurns;
    }

    public void setTotalEstimatedTurns(int totalEstimatedTurns) {
        this.totalEstimatedTurns = totalEstimatedTurns;
        updateProgress();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public List<String> getActionHistory() {
        return actionHistory;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    private void updateProgress() {
        if (totalEstimatedTurns > 0) {
            this.progressPercentage = Math.min(100.0, (double) currentTurn / totalEstimatedTurns * 100.0);
        }
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
}
