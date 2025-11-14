package com.crafto.ai.memory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * LongTermMemory provides persistent learning capabilities across game sessions
 * with strategy optimization and player behavior analysis.
 */
public class LongTermMemory {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();
    
    private static final String DEFAULT_MEMORY_DIR = "config/crafto/memory/";
    private static final String STRATEGIES_FILE = "strategies.json";
    private static final String PLAYER_PATTERNS_FILE = "player_patterns.json";
    private static final String FEEDBACK_FILE = "feedback.json";
    
    // Instance-specific directory
    private final String memoryDir;
    
    // Thread-safe storage for runtime data
    private final Map<String, SuccessfulStrategy> strategies = new ConcurrentHashMap<>();
    private final Map<String, PlayerPattern> playerPatterns = new ConcurrentHashMap<>();
    private final Map<String, List<FeedbackEntry>> feedback = new ConcurrentHashMap<>();
    
    // Configuration
    private final double MIN_SUCCESS_RATE = 0.3;
    private final double RECOMMENDED_SUCCESS_RATE = 0.7;
    private final long DATA_RETENTION_DAYS = 30;
    
    public LongTermMemory() {
        this(DEFAULT_MEMORY_DIR);
    }
    
    public LongTermMemory(String customMemoryDir) {
        this.memoryDir = customMemoryDir;
        ensureDirectoryExists();
        loadAllData();
    }
    
    /**
     * Records a successful strategy for future use
     * Requirement 1.1: Save strategy used to persistent storage when task completed successfully
     */
    public void recordSuccessfulStrategy(String taskType, String context, String strategy, double successRate) {
        if (taskType == null || strategy == null) {
            throw new IllegalArgumentException("TaskType and strategy cannot be null");
        }
        
        String key = generateStrategyKey(taskType, context, strategy);
        SuccessfulStrategy existing = strategies.get(key);
        
        if (existing == null) {
            existing = new SuccessfulStrategy(taskType, context, strategy, successRate);
            strategies.put(key, existing);
        } else {
            // Update existing strategy
            existing.updateSuccess(successRate);
        }
        
        saveStrategies();
    }
    
    /**
     * Retrieves the best strategy for a given task type and context
     * Requirement 1.6: Recommend strategy with highest success rate above 70%
     */
    public Optional<SuccessfulStrategy> getBestStrategy(String taskType, String context) {
        if (taskType == null) {
            return Optional.empty();
        }
        
        return strategies.values().stream()
                .filter(s -> s.getTaskType().equals(taskType))
                .filter(s -> context == null || s.getContext().equals(context))
                .filter(s -> s.getSuccessRate() >= RECOMMENDED_SUCCESS_RATE)
                .max(Comparator.comparing(SuccessfulStrategy::getSuccessRate));
    }
    
    /**
     * Records player behavior patterns for analysis
     * Requirement 1.2: Record behavior patterns and preferences when player interacts
     */
    public void recordPlayerBehavior(String playerName, String action, String context, boolean wasSuccessful) {
        if (playerName == null || action == null) {
            throw new IllegalArgumentException("PlayerName and action cannot be null");
        }
        
        PlayerPattern pattern = playerPatterns.computeIfAbsent(playerName, PlayerPattern::new);
        pattern.recordBehavior(action, context, wasSuccessful);
        
        savePlayerPatterns();
    }
    
    /**
     * Gets player preferences based on recorded behavior patterns
     */
    public PlayerPreferences getPlayerPreferences(String playerName) {
        PlayerPattern pattern = playerPatterns.get(playerName);
        if (pattern == null) {
            return new PlayerPreferences(playerName);
        }
        return pattern.calculatePreferences();
    }
    
    /**
     * Records player feedback for strategy improvement
     * Requirement 1.4: Update strategy ratings when receiving player feedback
     */
    public void recordFeedback(String playerName, String craftoName, String task, int rating, String comment) {
        if (playerName == null || craftoName == null || task == null) {
            throw new IllegalArgumentException("PlayerName, craftoName, and task cannot be null");
        }
        
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        FeedbackEntry entry = new FeedbackEntry(playerName, craftoName, task, rating, comment);
        feedback.computeIfAbsent(task, k -> new ArrayList<>()).add(entry);
        
        // Update related strategies based on feedback
        updateStrategiesFromFeedback(task, rating);
        
        saveFeedback();
    }
    
    /**
     * Provides learning statistics and analytics
     * Requirement 4.1: Provide learning statistics including success rates, strategy counts, and improvement metrics
     */
    public LearningStats getLearningStats() {
        int totalStrategies = strategies.size();
        int trackedPlayers = playerPatterns.size();
        int feedbackEntries = feedback.values().stream().mapToInt(List::size).sum();
        
        double averageSuccessRate = strategies.values().stream()
                .mapToDouble(SuccessfulStrategy::getSuccessRate)
                .average()
                .orElse(0.0);
        
        String mostSuccessfulTaskType = strategies.values().stream()
                .collect(Collectors.groupingBy(SuccessfulStrategy::getTaskType))
                .entrySet().stream()
                .max(Comparator.comparing(entry -> entry.getValue().stream()
                        .mapToDouble(SuccessfulStrategy::getSuccessRate)
                        .average().orElse(0.0)))
                .map(Map.Entry::getKey)
                .orElse("None");
        
        return new LearningStats(totalStrategies, trackedPlayers, feedbackEntries, 
                                averageSuccessRate, mostSuccessfulTaskType);
    }
    
    /**
     * Cleans up old and unused data
     * Requirement 4.4: Automatically archive or remove outdated learning data after 30 days
     */
    public void cleanupOldData() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(DATA_RETENTION_DAYS);
        
        // Remove old strategies with low success rates
        strategies.entrySet().removeIf(entry -> {
            SuccessfulStrategy strategy = entry.getValue();
            return strategy.getLastUsed().isBefore(cutoffTime) && 
                   strategy.getSuccessRate() < MIN_SUCCESS_RATE;
        });
        
        // Remove old player patterns
        playerPatterns.entrySet().removeIf(entry -> 
            entry.getValue().getLastInteraction().isBefore(cutoffTime));
        
        // Remove old feedback
        feedback.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(feedbackEntry -> 
                feedbackEntry.getTimestamp().isBefore(cutoffTime));
            return entry.getValue().isEmpty();
        });
        
        saveAllData();
    }
    
    // Private helper methods
    
    private String generateStrategyKey(String taskType, String context, String strategy) {
        return taskType + "_" + (context != null ? context.hashCode() : "default") + "_" + strategy.hashCode();
    }
    
    private void updateStrategiesFromFeedback(String task, int rating) {
        // Find strategies related to this task and adjust their success rates
        strategies.values().stream()
                .filter(s -> s.getTaskType().equals(task))
                .forEach(s -> {
                    // Adjust success rate based on feedback (simple implementation)
                    double adjustment = (rating - 3.0) * 0.1; // -0.2 to +0.2 adjustment
                    double newRate = Math.max(0.0, Math.min(1.0, s.getSuccessRate() + adjustment));
                    s.setSuccessRate(newRate);
                });
        
        saveStrategies();
    }
    
    // File I/O methods
    
    private void ensureDirectoryExists() {
        File dir = new File(memoryDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    private void loadAllData() {
        loadStrategies();
        loadPlayerPatterns();
        loadFeedback();
    }
    
    private void saveAllData() {
        saveStrategies();
        savePlayerPatterns();
        saveFeedback();
    }
    
    private void loadStrategies() {
        try {
            File file = new File(memoryDir + STRATEGIES_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, SuccessfulStrategy>>(){}.getType();
                Map<String, SuccessfulStrategy> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    strategies.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load strategies: " + e.getMessage());
        }
    }
    
    private void saveStrategies() {
        try (FileWriter writer = new FileWriter(memoryDir + STRATEGIES_FILE)) {
            GSON.toJson(strategies, writer);
        } catch (IOException e) {
            System.err.println("Failed to save strategies: " + e.getMessage());
        }
    }
    
    private void loadPlayerPatterns() {
        try {
            File file = new File(memoryDir + PLAYER_PATTERNS_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, PlayerPattern>>(){}.getType();
                Map<String, PlayerPattern> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    playerPatterns.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load player patterns: " + e.getMessage());
        }
    }
    
    private void savePlayerPatterns() {
        try (FileWriter writer = new FileWriter(memoryDir + PLAYER_PATTERNS_FILE)) {
            GSON.toJson(playerPatterns, writer);
        } catch (IOException e) {
            System.err.println("Failed to save player patterns: " + e.getMessage());
        }
    }
    
    private void loadFeedback() {
        try {
            File file = new File(memoryDir + FEEDBACK_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, List<FeedbackEntry>>>(){}.getType();
                Map<String, List<FeedbackEntry>> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    feedback.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load feedback: " + e.getMessage());
        }
    }
    
    private void saveFeedback() {
        try (FileWriter writer = new FileWriter(memoryDir + FEEDBACK_FILE)) {
            GSON.toJson(feedback, writer);
        } catch (IOException e) {
            System.err.println("Failed to save feedback: " + e.getMessage());
        }
    }
}