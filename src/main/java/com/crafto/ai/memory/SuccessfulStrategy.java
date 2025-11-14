package com.crafto.ai.memory;

import java.time.LocalDateTime;

/**
 * Data model for storing successful strategies with metadata
 * Used by LongTermMemory for strategy optimization
 */
public class SuccessfulStrategy {
    private String taskType;
    private String context;
    private String strategy;
    private double successRate;
    private LocalDateTime lastUsed;
    private int usageCount;
    private LocalDateTime creationTime;
    
    // Default constructor for JSON deserialization
    public SuccessfulStrategy() {
    }
    
    public SuccessfulStrategy(String taskType, String context, String strategy, double successRate) {
        this.taskType = taskType;
        this.context = context != null ? context : "default";
        this.strategy = strategy;
        this.successRate = Math.max(0.0, Math.min(1.0, successRate)); // Clamp between 0 and 1
        this.lastUsed = LocalDateTime.now();
        this.usageCount = 1;
        this.creationTime = LocalDateTime.now();
    }
    
    /**
     * Updates the strategy with new success information
     */
    public void updateSuccess(double newSuccessRate) {
        // Calculate weighted average of success rates
        double weight = 0.3; // Weight for new data
        this.successRate = (1 - weight) * this.successRate + weight * newSuccessRate;
        this.successRate = Math.max(0.0, Math.min(1.0, this.successRate)); // Clamp between 0 and 1
        this.lastUsed = LocalDateTime.now();
        this.usageCount++;
    }
    
    /**
     * Marks the strategy as used
     */
    public void markAsUsed() {
        this.lastUsed = LocalDateTime.now();
        this.usageCount++;
    }
    
    // Getters and setters
    
    public String getTaskType() {
        return taskType;
    }
    
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public String getStrategy() {
        return strategy;
    }
    
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
    
    public double getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(double successRate) {
        this.successRate = Math.max(0.0, Math.min(1.0, successRate));
    }
    
    public LocalDateTime getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }
    
    @Override
    public String toString() {
        return String.format("SuccessfulStrategy{taskType='%s', context='%s', successRate=%.2f, usageCount=%d}", 
                           taskType, context, successRate, usageCount);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SuccessfulStrategy that = (SuccessfulStrategy) obj;
        return taskType.equals(that.taskType) && 
               context.equals(that.context) && 
               strategy.equals(that.strategy);
    }
    
    @Override
    public int hashCode() {
        return taskType.hashCode() + context.hashCode() + strategy.hashCode();
    }
}