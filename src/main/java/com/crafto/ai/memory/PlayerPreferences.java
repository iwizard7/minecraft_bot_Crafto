package com.crafto.ai.memory;

import java.util.*;

/**
 * Data model representing calculated player preferences
 * Generated from PlayerPattern analysis
 */
public class PlayerPreferences {
    private String playerName;
    private List<String> preferredActions;
    private List<String> successfulActions;
    private Map<String, String> contextualPreferences;
    private double activityLevel;
    private String communicationStyle;
    
    public PlayerPreferences() {
        this.preferredActions = new ArrayList<>();
        this.successfulActions = new ArrayList<>();
        this.contextualPreferences = new HashMap<>();
        this.communicationStyle = "normal";
    }
    
    public PlayerPreferences(String playerName) {
        this();
        this.playerName = playerName;
    }
    
    // Getters and setters
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public List<String> getPreferredActions() {
        return preferredActions;
    }
    
    public void setPreferredActions(List<String> preferredActions) {
        this.preferredActions = preferredActions != null ? preferredActions : new ArrayList<>();
    }
    
    public List<String> getSuccessfulActions() {
        return successfulActions;
    }
    
    public void setSuccessfulActions(List<String> successfulActions) {
        this.successfulActions = successfulActions != null ? successfulActions : new ArrayList<>();
    }
    
    public Map<String, String> getContextualPreferences() {
        return contextualPreferences;
    }
    
    public void setContextualPreferences(Map<String, String> contextualPreferences) {
        this.contextualPreferences = contextualPreferences != null ? contextualPreferences : new HashMap<>();
    }
    
    public double getActivityLevel() {
        return activityLevel;
    }
    
    public void setActivityLevel(double activityLevel) {
        this.activityLevel = activityLevel;
    }
    
    public String getCommunicationStyle() {
        return communicationStyle;
    }
    
    public void setCommunicationStyle(String communicationStyle) {
        this.communicationStyle = communicationStyle != null ? communicationStyle : "normal";
    }
    
    /**
     * Checks if the player prefers a specific action
     */
    public boolean prefersAction(String action) {
        return preferredActions.contains(action);
    }
    
    /**
     * Checks if the player is successful with a specific action
     */
    public boolean isSuccessfulWith(String action) {
        return successfulActions.contains(action);
    }
    
    /**
     * Gets the preferred context for an action
     */
    public String getPreferredContext(String action) {
        return contextualPreferences.get(action);
    }
    
    @Override
    public String toString() {
        return String.format("PlayerPreferences{playerName='%s', preferredActions=%s, activityLevel=%.2f}", 
                           playerName, preferredActions, activityLevel);
    }
}