package com.crafto.ai.optimization;

import com.crafto.ai.CraftoMod;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SystemMonitor {
    private static SystemMonitor instance;
    
    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    private final ThreadMXBean threadBean;
    private final ScheduledExecutorService scheduler;
    
    private volatile double cpuUsage = 0.0;
    private volatile long memoryUsed = 0;
    private volatile long memoryMax = 0;
    private volatile int threadCount = 0;
    private volatile boolean systemUnderLoad = false;
    
    // Пороговые значения для определения нагрузки
    private static final double CPU_THRESHOLD = 0.8; // 80%
    private static final double MEMORY_THRESHOLD = 0.85; // 85%
    private static final int THREAD_THRESHOLD = 100;
    
    private SystemMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        startMonitoring();
    }
    
    public static synchronized SystemMonitor getInstance() {
        if (instance == null) {
            instance = new SystemMonitor();
        }
        return instance;
    }
    
    private void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::updateMetrics, 0, 5, TimeUnit.SECONDS);
    }
    
    private void updateMetrics() {
        try {
            // Обновляем метрики памяти
            memoryUsed = memoryBean.getHeapMemoryUsage().getUsed();
            memoryMax = memoryBean.getHeapMemoryUsage().getMax();
            
            // Обновляем метрики CPU (если доступно)
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                cpuUsage = sunOsBean.getProcessCpuLoad();
            }
            
            // Обновляем количество потоков
            threadCount = threadBean.getThreadCount();
            
            // Определяем состояние нагрузки системы
            boolean wasUnderLoad = systemUnderLoad;
            systemUnderLoad = isSystemUnderLoad();
            
            // Логируем изменения состояния
            if (systemUnderLoad != wasUnderLoad) {
                if (systemUnderLoad) {
                    CraftoMod.LOGGER.warn("System is under high load - CPU: {:.1f}%, Memory: {:.1f}%, Threads: {}", 
                        cpuUsage * 100, getMemoryUsagePercent(), threadCount);
                } else {
                    CraftoMod.LOGGER.info("System load normalized - CPU: {:.1f}%, Memory: {:.1f}%, Threads: {}", 
                        cpuUsage * 100, getMemoryUsagePercent(), threadCount);
                }
            }
            
        } catch (Exception e) {
            CraftoMod.LOGGER.error("Error updating system metrics", e);
        }
    }
    
    public boolean isSystemUnderLoad() {
        double memoryPercent = getMemoryUsagePercent();
        return cpuUsage > CPU_THRESHOLD || 
               memoryPercent > MEMORY_THRESHOLD || 
               threadCount > THREAD_THRESHOLD;
    }
    
    public double getCpuUsage() {
        return cpuUsage;
    }
    
    public double getMemoryUsagePercent() {
        if (memoryMax <= 0) return 0.0;
        return (double) memoryUsed / memoryMax;
    }
    
    public long getMemoryUsedMB() {
        return memoryUsed / (1024 * 1024);
    }
    
    public long getMemoryMaxMB() {
        return memoryMax / (1024 * 1024);
    }
    
    public int getThreadCount() {
        return threadCount;
    }
    
    public SystemLoadLevel getLoadLevel() {
        if (isSystemUnderLoad()) {
            double memoryPercent = getMemoryUsagePercent();
            if (cpuUsage > 0.95 || memoryPercent > 0.95) {
                return SystemLoadLevel.CRITICAL;
            } else if (cpuUsage > 0.9 || memoryPercent > 0.9) {
                return SystemLoadLevel.HIGH;
            } else {
                return SystemLoadLevel.MODERATE;
            }
        }
        return SystemLoadLevel.LOW;
    }
    
    public OptimizationRecommendation getOptimizationRecommendation() {
        SystemLoadLevel loadLevel = getLoadLevel();
        
        return switch (loadLevel) {
            case CRITICAL -> new OptimizationRecommendation(
                "Reduce concurrent AI requests to 1",
                "Increase cache expiration time",
                "Consider pausing non-essential agents"
            );
            case HIGH -> new OptimizationRecommendation(
                "Reduce concurrent AI requests to 2",
                "Increase cache hit rate",
                "Optimize memory usage"
            );
            case MODERATE -> new OptimizationRecommendation(
                "Monitor system closely",
                "Consider reducing batch sizes",
                "Optimize agent strategies"
            );
            case LOW -> new OptimizationRecommendation(
                "System running optimally",
                "Can increase concurrent requests if needed",
                "Good time for agent training"
            );
        };
    }
    
    public void logSystemStatus() {
        CraftoMod.LOGGER.info("=== System Status ===");
        CraftoMod.LOGGER.info("CPU Usage: {:.1f}%", cpuUsage * 100);
        CraftoMod.LOGGER.info("Memory Usage: {:.1f}% ({} MB / {} MB)", 
            getMemoryUsagePercent() * 100, getMemoryUsedMB(), getMemoryMaxMB());
        CraftoMod.LOGGER.info("Thread Count: {}", threadCount);
        CraftoMod.LOGGER.info("Load Level: {}", getLoadLevel());
        CraftoMod.LOGGER.info("Under Load: {}", systemUnderLoad);
        
        OptimizationRecommendation rec = getOptimizationRecommendation();
        CraftoMod.LOGGER.info("Recommendations:");
        rec.recommendations.forEach(r -> CraftoMod.LOGGER.info("  - {}", r));
    }
    
    // Принудительная сборка мусора при критической нагрузке
    public void forceGarbageCollection() {
        if (getLoadLevel() == SystemLoadLevel.CRITICAL) {
            CraftoMod.LOGGER.warn("Forcing garbage collection due to critical system load");
            System.gc();
            
            // Ждем немного и обновляем метрики
            try {
                Thread.sleep(1000);
                updateMetrics();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public enum SystemLoadLevel {
        LOW, MODERATE, HIGH, CRITICAL
    }
    
    public static class OptimizationRecommendation {
        public final java.util.List<String> recommendations;
        
        public OptimizationRecommendation(String... recommendations) {
            this.recommendations = java.util.List.of(recommendations);
        }
    }
}