package com.crafto.ai.util;

import com.crafto.ai.CraftoMod;

/**
 * Simple performance monitoring for M2 MacBook optimization
 */
public class PerformanceMonitor {
    private static long lastMemoryCheck = 0;
    private static final long MEMORY_CHECK_INTERVAL = 30000; // 30 seconds
    
    public static void checkMemoryUsage() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMemoryCheck < MEMORY_CHECK_INTERVAL) {
            return;
        }
        
        lastMemoryCheck = currentTime;
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        double usedPercentage = (double) usedMemory / maxMemory * 100;
        
        if (usedPercentage > 80) {
            CraftoMod.LOGGER.warn("High memory usage: {:.1f}% ({} MB / {} MB)", 
                usedPercentage, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
            
            // Suggest garbage collection if memory is very high
            if (usedPercentage > 90) {
                System.gc();
                CraftoMod.LOGGER.info("Triggered garbage collection due to high memory usage");
            }
        }
    }
    
    public static void logPerformanceMetrics(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        if (duration > 1000) { // Log operations taking more than 1 second
            CraftoMod.LOGGER.warn("Slow operation '{}' took {} ms", operation, duration);
        }
    }
}