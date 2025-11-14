package com.crafto.ai.memory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data model for tracking player behavior patterns and preferences
 * Used by LongTermMemory for player behavior analysis
 */
public class PlayerPattern {
    private String playerName;
    private Map<String, Integer> actionFrequency;
    private Map<String, Double> actionSuccessRates;
    private Map<String, String> contextualPreferences;
    private LocalDateTime lastInteraction;
    private LocalDateTime firstInteraction;
    private int totalInteractions;
    
    // Default constructor for JSON deserialization
    public PlayerPattern() {
        this.actionFrequency = new ConcurrentHashMap<>();
        this.actionSuccessRates = new ConcurrentHashMap<>();
        this.contextualPreferences = new ConcurrentHashMap<>();
    }
    
    public PlayerPattern(String playerName) {
        this.playerName = playerName;
        this.actionFrequency = new ConcurrentHashMap<>();
        this.actionSuccessRates = new ConcurrentHashMap<>();
        this.contextualPreferences = new ConcurrentHashMap<>();
        this.lastInteraction = LocalDateTime.now();
        this.firstInteraction = LocalDateTime.now();
        this.totalInteractions = 0;
    }
    
    /**
     * Records a behavior instance for this player
     */
    public void recordBehavior(String action, String context, boolean wasSuccessful) {
        if (action == null) return;
        
        // Update frequency
        actionFrequency.merge(action, 1, Integer::sum);
        
        // Update success rate
        String key = action + (context != null ? "_" + context : "");
        double currentRate = actionSuccessRates.getOrDefault(key, 0.5);
        double newRate = wasSuccessful ? 1.0 : 0.0;
        double weight = 0.2; // Weight for new data
        actionSuccessRates.put(key, (1 - weight) * currentRate + weight * newRate);
        
        // Update contextual preferences
        if (context != null && wasSuccessful) {
            contextualPreferences.put(action, context);
        }
        
        // Update metadata
        this.lastInteraction = LocalDateTime.now();
        this.totalInteractions++;
    }
    
    /**
     * Calculates player preferences based on recorded patterns
     */
    public PlayerPreferences calculatePreferences() {
        PlayerPreferences preferences = new PlayerPreferences(playerName);
        
        // Find most frequent actions
        List<String> preferredActions = actionFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
        preferences.setPreferredActions(preferredActions);
        
        // Find most successful actions
        List<String> successfulActions = actionSuccessRates.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.7)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
        preferences.setSuccessfulActions(successfulActions);
        
        // Set contextual preferences
        preferences.setContextualPreferences(new HashMap<>(contextualPreferences));
        
        // Calculate activity level
        long daysSinceFirst = java.time.temporal.ChronoUnit.DAYS.between(firstInteraction, LocalDateTime.now());
        double activityLevel = daysSinceFirst > 0 ? (double) totalInteractions / daysSinceFirst : totalInteractions;
        preferences.setActivityLevel(activityLevel);
        
        return preferences;
    }
    
    /**
     * Gets the success rate for a specific action in a context
     */
    public double getSuccessRate(String action, String context) {
        String key = action + (context != null ? "_" + context : "");
        return actionSuccessRates.getOrDefault(key, 0.5);
    }
    
    /**
     * Gets the frequency of a specific action
     */
    public int getActionFrequency(String action) {
        return actionFrequency.getOrDefault(action, 0);
    }
    
    /**
     * Gets the preferred context for an action
     */
    public String getPreferredContext(String action) {
        return contextualPreferences.get(action);
    }
    
    // Getters and setters
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public Map<String, Integer> getActionFrequency() {
        return actionFrequency;
    }
    
    public void setActionFrequency(Map<String, Integer> actionFrequency) {
        this.actionFrequency = actionFrequency;
    }
    
    public Map<String, Double> getActionSuccessRates() {
        return actionSuccessRates;
    }
    
    public void setActionSuccessRates(Map<String, Double> actionSuccessRates) {
        this.actionSuccessRates = actionSuccessRates;
    }
    
    public Map<String, String> getContextualPreferences() {
        return contextualPreferences;
    }
    
    public void setContextualPreferences(Map<String, String> contextualPreferences) {
        this.contextualPreferences = contextualPreferences;
    }
    
    public LocalDateTime getLastInteraction() {
        return lastInteraction;
    }
    
    public void setLastInteraction(LocalDateTime lastInteraction) {
        this.lastInteraction = lastInteraction;
    }
    
    public LocalDateTime getFirstInteraction() {
        return firstInteraction;
    }
    
    public void setFirstInteraction(LocalDateTime firstInteraction) {
        this.firstInteraction = firstInteraction;
    }
    
    public int getTotalInteractions() {
        return totalInteractions;
    }
    
    public void setTotalInteractions(int totalInteractions) {
        this.totalInteractions = totalInteractions;
    }
    
    @Override
    public String toString() {
        return String.format("PlayerPattern{playerName='%s', totalInteractions=%d, lastInteraction=%s}", 
                           playerName, totalInteractions, lastInteraction);
    }
}