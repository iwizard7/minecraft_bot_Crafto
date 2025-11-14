package com.crafto.ai.memory;

/**
 * Data model for learning analytics and statistics
 * Provides insights into the learning system's performance
 */
public class LearningStats {
    private int totalStrategies;
    private int trackedPlayers;
    private int feedbackEntries;
    private double averageSuccessRate;
    private String mostSuccessfulTaskType;
    private long memoryUsageBytes;
    private double improvementTrend;
    
    // Default constructor for JSON deserialization
    public LearningStats() {
    }
    
    public LearningStats(int totalStrategies, int trackedPlayers, int feedbackEntries, 
                        double averageSuccessRate, String mostSuccessfulTaskType) {
        this.totalStrategies = totalStrategies;
        this.trackedPlayers = trackedPlayers;
        this.feedbackEntries = feedbackEntries;
        this.averageSuccessRate = averageSuccessRate;
        this.mostSuccessfulTaskType = mostSuccessfulTaskType;
        this.memoryUsageBytes = 0;
        this.improvementTrend = 0.0;
    }
    
    /**
     * Calculates the learning efficiency as a percentage
     */
    public double getLearningEfficiency() {
        if (totalStrategies == 0) return 0.0;
        return (averageSuccessRate * 100.0);
    }
    
    /**
     * Gets the feedback ratio (feedback entries per strategy)
     */
    public double getFeedbackRatio() {
        if (totalStrategies == 0) return 0.0;
        return (double) feedbackEntries / totalStrategies;
    }
    
    /**
     * Gets the player engagement level (strategies per player)
     */
    public double getPlayerEngagement() {
        if (trackedPlayers == 0) return 0.0;
        return (double) totalStrategies / trackedPlayers;
    }
    
    // Getters and setters
    
    public int getTotalStrategies() {
        return totalStrategies;
    }
    
    public void setTotalStrategies(int totalStrategies) {
        this.totalStrategies = totalStrategies;
    }
    
    public int getTrackedPlayers() {
        return trackedPlayers;
    }
    
    public void setTrackedPlayers(int trackedPlayers) {
        this.trackedPlayers = trackedPlayers;
    }
    
    public int getFeedbackEntries() {
        return feedbackEntries;
    }
    
    public void setFeedbackEntries(int feedbackEntries) {
        this.feedbackEntries = feedbackEntries;
    }
    
    public double getAverageSuccessRate() {
        return averageSuccessRate;
    }
    
    public void setAverageSuccessRate(double averageSuccessRate) {
        this.averageSuccessRate = averageSuccessRate;
    }
    
    public String getMostSuccessfulTaskType() {
        return mostSuccessfulTaskType;
    }
    
    public void setMostSuccessfulTaskType(String mostSuccessfulTaskType) {
        this.mostSuccessfulTaskType = mostSuccessfulTaskType;
    }
    
    public long getMemoryUsageBytes() {
        return memoryUsageBytes;
    }
    
    public void setMemoryUsageBytes(long memoryUsageBytes) {
        this.memoryUsageBytes = memoryUsageBytes;
    }
    
    public double getImprovementTrend() {
        return improvementTrend;
    }
    
    public void setImprovementTrend(double improvementTrend) {
        this.improvementTrend = improvementTrend;
    }
    
    @Override
    public String toString() {
        return String.format("LearningStats{strategies=%d, players=%d, feedback=%d, avgSuccess=%.2f%%, mostSuccessful='%s'}", 
                           totalStrategies, trackedPlayers, feedbackEntries, 
                           averageSuccessRate * 100, mostSuccessfulTaskType);
    }
}