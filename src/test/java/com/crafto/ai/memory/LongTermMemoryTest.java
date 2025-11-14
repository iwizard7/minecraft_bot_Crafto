package com.crafto.ai.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LongTermMemory functionality
 */
public class LongTermMemoryTest {
    
    private LongTermMemory longTermMemory;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Set up temporary directory for testing
        longTermMemory = new LongTermMemory(tempDir.toString() + "/");
    }
    
    @Test
    void testRecordAndRetrieveStrategy() {
        // Test recording a successful strategy
        String taskType = "BUILD_HOUSE";
        String context = "plains_biome";
        String strategy = "gather_wood_first";
        double successRate = 0.85;
        
        longTermMemory.recordSuccessfulStrategy(taskType, context, strategy, successRate);
        
        // Test retrieving the strategy
        Optional<SuccessfulStrategy> retrieved = longTermMemory.getBestStrategy(taskType, context);
        
        assertTrue(retrieved.isPresent());
        assertEquals(taskType, retrieved.get().getTaskType());
        assertEquals(context, retrieved.get().getContext());
        assertEquals(strategy, retrieved.get().getStrategy());
        assertEquals(successRate, retrieved.get().getSuccessRate(), 0.01);
    }
    
    @Test
    void testPlayerBehaviorRecording() {
        String playerName = "TestPlayer";
        String action = "build_command";
        String context = "day_time";
        
        // Record successful behavior
        longTermMemory.recordPlayerBehavior(playerName, action, context, true);
        
        // Get player preferences
        PlayerPreferences preferences = longTermMemory.getPlayerPreferences(playerName);
        
        assertNotNull(preferences);
        assertEquals(playerName, preferences.getPlayerName());
        assertTrue(preferences.getActivityLevel() > 0);
    }
    
    @Test
    void testFeedbackRecording() {
        String playerName = "TestPlayer";
        String craftoName = "TestCrafto";
        String task = "BUILD_HOUSE";
        int rating = 4;
        String comment = "Good job!";
        
        // Record feedback
        longTermMemory.recordFeedback(playerName, craftoName, task, rating, comment);
        
        // Verify learning stats updated
        LearningStats stats = longTermMemory.getLearningStats();
        assertEquals(1, stats.getFeedbackEntries());
    }
    
    @Test
    void testLearningStats() {
        // Record some test data
        longTermMemory.recordSuccessfulStrategy("BUILD", "test", "strategy1", 0.8);
        longTermMemory.recordSuccessfulStrategy("MINE", "test", "strategy2", 0.9);
        longTermMemory.recordPlayerBehavior("player1", "action1", "context1", true);
        longTermMemory.recordFeedback("player1", "crafto1", "BUILD", 5, "Excellent");
        
        LearningStats stats = longTermMemory.getLearningStats();
        
        assertEquals(2, stats.getTotalStrategies());
        assertEquals(1, stats.getTrackedPlayers());
        assertEquals(1, stats.getFeedbackEntries());
        assertTrue(stats.getAverageSuccessRate() > 0.8);
    }
    
    @Test
    void testInvalidInputHandling() {
        // Test null inputs
        assertThrows(IllegalArgumentException.class, () -> 
            longTermMemory.recordSuccessfulStrategy(null, "context", "strategy", 0.5));
        
        assertThrows(IllegalArgumentException.class, () -> 
            longTermMemory.recordPlayerBehavior(null, "action", "context", true));
        
        assertThrows(IllegalArgumentException.class, () -> 
            longTermMemory.recordFeedback(null, "crafto", "task", 3, "comment"));
        
        // Test invalid rating
        assertThrows(IllegalArgumentException.class, () -> 
            longTermMemory.recordFeedback("player", "crafto", "task", 6, "comment"));
    }
    
    @Test
    void testStrategyFiltering() {
        // Record strategies with different success rates but same task type and context
        longTermMemory.recordSuccessfulStrategy("TEST", "context1", "low_success", 0.2);
        longTermMemory.recordSuccessfulStrategy("TEST", "context1", "high_success", 0.8);
        
        // Should return the high success strategy (>= 0.7)
        Optional<SuccessfulStrategy> best = longTermMemory.getBestStrategy("TEST", "context1");
        
        assertTrue(best.isPresent());
        assertEquals("high_success", best.get().getStrategy());
    }
}