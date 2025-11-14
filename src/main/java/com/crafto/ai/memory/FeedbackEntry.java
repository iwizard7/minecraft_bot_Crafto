package com.crafto.ai.memory;

import java.time.LocalDateTime;

/**
 * Data model for storing player feedback entries
 * Used by LongTermMemory for strategy improvement based on player input
 */
public class FeedbackEntry {
    private String playerName;
    private String craftoName;
    private String task;
    private int rating; // 1-5 scale
    private String comment;
    private LocalDateTime timestamp;
    private String context;
    
    // Default constructor for JSON deserialization
    public FeedbackEntry() {
    }
    
    public FeedbackEntry(String playerName, String craftoName, String task, int rating, String comment) {
        this.playerName = playerName;
        this.craftoName = craftoName;
        this.task = task;
        this.rating = Math.max(1, Math.min(5, rating)); // Clamp between 1 and 5
        this.comment = comment;
        this.timestamp = LocalDateTime.now();
    }
    
    public FeedbackEntry(String playerName, String craftoName, String task, int rating, String comment, String context) {
        this(playerName, craftoName, task, rating, comment);
        this.context = context;
    }
    
    /**
     * Checks if this feedback is positive (rating >= 4)
     */
    public boolean isPositive() {
        return rating >= 4;
    }
    
    /**
     * Checks if this feedback is negative (rating <= 2)
     */
    public boolean isNegative() {
        return rating <= 2;
    }
    
    /**
     * Gets the feedback impact as a value between -1 and 1
     * Used for adjusting strategy success rates
     */
    public double getImpact() {
        return (rating - 3.0) / 2.0; // Maps 1-5 to -1 to 1
    }
    
    // Getters and setters
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public String getCraftoName() {
        return craftoName;
    }
    
    public void setCraftoName(String craftoName) {
        this.craftoName = craftoName;
    }
    
    public String getTask() {
        return task;
    }
    
    public void setTask(String task) {
        this.task = task;
    }
    
    public int getRating() {
        return rating;
    }
    
    public void setRating(int rating) {
        this.rating = Math.max(1, Math.min(5, rating));
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    @Override
    public String toString() {
        return String.format("FeedbackEntry{playerName='%s', craftoName='%s', task='%s', rating=%d, timestamp=%s}", 
                           playerName, craftoName, task, rating, timestamp);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FeedbackEntry that = (FeedbackEntry) obj;
        return playerName.equals(that.playerName) && 
               craftoName.equals(that.craftoName) && 
               task.equals(that.task) && 
               timestamp.equals(that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return playerName.hashCode() + craftoName.hashCode() + task.hashCode() + timestamp.hashCode();
    }
}