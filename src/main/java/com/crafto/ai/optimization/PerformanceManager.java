package com.crafto.ai.optimization;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.memory.AgentMemory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceManager {
    private static PerformanceManager instance;
    
    private final Map<String, AgentMemory> agentMemories = new ConcurrentHashMap<>();
    private final AIRequestBatcher requestBatcher = new AIRequestBatcher();
    private final SystemMonitor systemMonitor = SystemMonitor.getInstance();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Метрики производительности
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong averageResponseTime = new AtomicLong(0);
    private final Map<String, Long> commandExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> commandSuccessRates = new ConcurrentHashMap<>();
    
    // Настройки оптимизации
    private volatile boolean adaptiveOptimization = true;
    private volatile int maxConcurrentRequests = 3;
    private volatile long cacheExpirationTime = 30 * 60 * 1000L; // 30 минут
    
    private PerformanceManager() {
        startPerformanceMonitoring();
        startMemoryCleanup();
        requestBatcher.preloadPopularCommands();
    }
    
    public static synchronized PerformanceManager getInstance() {
        if (instance == null) {
            instance = new PerformanceManager();
        }
        return instance;
    }
    
    // Получение памяти агента
    public AgentMemory getAgentMemory(String agentName) {
        return agentMemories.computeIfAbsent(agentName, AgentMemory::new);
    }
    
    // Оптимизированный запрос к AI
    public CompletableFuture<String> processAIRequest(String agentName, String command, String context) {
        long startTime = System.currentTimeMillis();
        totalRequests.incrementAndGet();
        
        return requestBatcher.submitRequest(agentName, command, context)
            .whenComplete((response, throwable) -> {
                long executionTime = System.currentTimeMillis() - startTime;
                recordRequestMetrics(command, executionTime, throwable == null);
                
                // Обновляем память агента
                AgentMemory memory = getAgentMemory(agentName);
                memory.recordCommandTime(command, executionTime);
                
                if (throwable == null) {
                    CraftoMod.LOGGER.debug("AI request completed in {}ms for agent: {}", 
                                        executionTime, agentName);
                } else {
                    CraftoMod.LOGGER.error("AI request failed for agent: " + agentName, throwable);
                }
            });
    }
    
    // Запись стратегии выполнения
    public void recordStrategy(String agentName, String taskType, String strategy, 
                              long executionTime, boolean success) {
        AgentMemory memory = getAgentMemory(agentName);
        memory.recordSuccessfulStrategy(taskType, strategy, executionTime, success);
        
        // Обновляем глобальные метрики
        String key = taskType + "_" + agentName;
        commandExecutionTimes.put(key, executionTime);
        commandSuccessRates.merge(key, success ? 1 : 0, Integer::sum);
    }
    
    // Получение оптимальной стратегии
    public Optional<String> getOptimalStrategy(String agentName, String taskType) {
        AgentMemory memory = getAgentMemory(agentName);
        return memory.getBestStrategy(taskType)
                    .map(strategy -> strategy.strategy);
    }
    
    // Запись взаимодействия с игроком
    public void recordPlayerInteraction(String agentName, String playerName, String command) {
        AgentMemory memory = getAgentMemory(agentName);
        memory.recordPlayerInteraction(playerName, command);
    }
    
    // Получение предпочтений игрока
    public Optional<AgentMemory.PlayerInteraction> getPlayerPreferences(String agentName, String playerName) {
        AgentMemory memory = getAgentMemory(agentName);
        return memory.getPlayerPreferences(playerName);
    }
    
    // Адаптивная оптимизация
    public void optimizePerformance() {
        if (!adaptiveOptimization) return;
        
        long avgResponseTime = averageResponseTime.get();
        double cacheHitRate = totalRequests.get() > 0 ? 
            (double) cacheHits.get() / totalRequests.get() : 0.0;
        
        // Получаем информацию о системной нагрузке
        SystemMonitor.SystemLoadLevel loadLevel = systemMonitor.getLoadLevel();
        SystemMonitor.OptimizationRecommendation sysRec = systemMonitor.getOptimizationRecommendation();
        
        CraftoMod.LOGGER.info("Performance metrics - Avg response: {}ms, Cache hit rate: {:.2f}%, System load: {}", 
                           avgResponseTime, cacheHitRate * 100, loadLevel);
        
        // Адаптивная настройка на основе системной нагрузки
        switch (loadLevel) {
            case CRITICAL -> {
                maxConcurrentRequests = 1;
                cacheExpirationTime = Math.min(60 * 60 * 1000L, cacheExpirationTime + 10 * 60 * 1000L);
                systemMonitor.forceGarbageCollection();
                CraftoMod.LOGGER.warn("CRITICAL load detected - reducing to 1 concurrent request");
            }
            case HIGH -> {
                maxConcurrentRequests = Math.max(1, Math.min(2, maxConcurrentRequests));
                cacheExpirationTime = Math.min(45 * 60 * 1000L, cacheExpirationTime + 5 * 60 * 1000L);
                CraftoMod.LOGGER.warn("HIGH load detected - limiting to 2 concurrent requests");
            }
            case MODERATE -> {
                // Стандартная логика оптимизации
                if (avgResponseTime > 10000) {
                    maxConcurrentRequests = Math.max(1, maxConcurrentRequests - 1);
                } else if (avgResponseTime < 5000 && cacheHitRate > 0.7) {
                    maxConcurrentRequests = Math.min(3, maxConcurrentRequests + 1);
                }
            }
            case LOW -> {
                // Можем быть более агрессивными в оптимизации
                if (avgResponseTime > 8000) {
                    maxConcurrentRequests = Math.max(2, maxConcurrentRequests - 1);
                } else if (avgResponseTime < 3000 && cacheHitRate > 0.7) {
                    maxConcurrentRequests = Math.min(5, maxConcurrentRequests + 1);
                }
            }
        }
        
        // Адаптивное время жизни кэша
        if (cacheHitRate < 0.3) { // Низкий hit rate
            cacheExpirationTime = Math.max(10 * 60 * 1000L, cacheExpirationTime - 5 * 60 * 1000L);
        } else if (cacheHitRate > 0.8) { // Высокий hit rate
            cacheExpirationTime = Math.min(60 * 60 * 1000L, cacheExpirationTime + 5 * 60 * 1000L);
        }
        
        CraftoMod.LOGGER.info("Optimization applied - Concurrent requests: {}, Cache time: {}min", 
                           maxConcurrentRequests, cacheExpirationTime / 60000);
    }
    
    // Предсказание времени выполнения команды
    public long predictExecutionTime(String agentName, String command) {
        AgentMemory memory = getAgentMemory(agentName);
        long historicalTime = memory.getAverageCommandTime(command);
        
        // Учитываем текущую нагрузку
        double loadFactor = Math.min(2.0, 1.0 + (totalRequests.get() % 10) * 0.1);
        
        return Math.round(historicalTime * loadFactor);
    }
    
    // Проверка готовности агента к новой задаче
    public boolean isAgentReady(String agentName, String taskType) {
        AgentMemory memory = getAgentMemory(agentName);
        
        // Проверяем историю успешности
        Optional<AgentMemory.SuccessfulStrategy> bestStrategy = memory.getBestStrategy(taskType);
        if (bestStrategy.isPresent() && bestStrategy.get().successRate < 0.3) {
            CraftoMod.LOGGER.warn("Agent {} has low success rate for task: {}", agentName, taskType);
            return false;
        }
        
        return true;
    }
    
    // Получение рекомендаций по оптимизации
    public List<String> getOptimizationRecommendations(String agentName) {
        List<String> recommendations = new ArrayList<>();
        AgentMemory memory = getAgentMemory(agentName);
        
        // Анализ стратегий
        long avgResponseTime = averageResponseTime.get();
        if (avgResponseTime > 8000) {
            recommendations.add("Consider using simpler commands to reduce AI response time");
        }
        
        double cacheHitRate = totalRequests.get() > 0 ? 
            (double) cacheHits.get() / totalRequests.get() : 0.0;
        if (cacheHitRate < 0.5) {
            recommendations.add("Try using more common commands to improve cache efficiency");
        }
        
        // Анализ локаций
        List<AgentMemory.LocationInfo> locations = memory.getExploredLocations();
        if (locations.size() < 3) {
            recommendations.add("Explore more areas to build location knowledge");
        }
        
        return recommendations;
    }
    
    private void recordRequestMetrics(String command, long executionTime, boolean success) {
        // Обновляем среднее время ответа
        long currentAvg = averageResponseTime.get();
        long newAvg = (currentAvg + executionTime) / 2;
        averageResponseTime.set(newAvg);
        
        // Записываем время выполнения команды
        commandExecutionTimes.put(command, executionTime);
        
        if (success) {
            commandSuccessRates.merge(command, 1, Integer::sum);
        }
    }
    
    private void startPerformanceMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                optimizePerformance();
                requestBatcher.logStatistics();
                logPerformanceMetrics();
            } catch (Exception e) {
                CraftoMod.LOGGER.error("Error in performance monitoring", e);
            }
        }, 60, 60, TimeUnit.SECONDS); // Каждую минуту
    }
    
    private void startMemoryCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Очистка старых данных из памяти агентов
                agentMemories.values().forEach(AgentMemory::cleanupOldData);
                
                // Очистка старых метрик
                cleanupOldMetrics();
                
                CraftoMod.LOGGER.debug("Memory cleanup completed");
            } catch (Exception e) {
                CraftoMod.LOGGER.error("Error in memory cleanup", e);
            }
        }, 24, 24, TimeUnit.HOURS); // Каждые 24 часа
    }
    
    private void cleanupOldMetrics() {
        // Очищаем метрики старше недели
        long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        
        // Здесь можно добавить логику очистки старых метрик
        // Пока просто сбрасываем счетчики если они слишком большие
        if (totalRequests.get() > 100000) {
            totalRequests.set(totalRequests.get() / 2);
            cacheHits.set(cacheHits.get() / 2);
        }
    }
    
    private void logPerformanceMetrics() {
        CraftoMod.LOGGER.info("=== Performance Metrics ===");
        CraftoMod.LOGGER.info("Total requests: {}", totalRequests.get());
        CraftoMod.LOGGER.info("Cache hits: {}", cacheHits.get());
        CraftoMod.LOGGER.info("Average response time: {}ms", averageResponseTime.get());
        CraftoMod.LOGGER.info("Active agents: {}", agentMemories.size());
        CraftoMod.LOGGER.info("Max concurrent requests: {}", maxConcurrentRequests);
        CraftoMod.LOGGER.info("Cache expiration time: {}ms", cacheExpirationTime);
        
        // Системные метрики
        CraftoMod.LOGGER.info("System CPU: {:.1f}%", systemMonitor.getCpuUsage() * 100);
        CraftoMod.LOGGER.info("System Memory: {:.1f}% ({} MB / {} MB)", 
            systemMonitor.getMemoryUsagePercent() * 100, 
            systemMonitor.getMemoryUsedMB(), 
            systemMonitor.getMemoryMaxMB());
        CraftoMod.LOGGER.info("System Threads: {}", systemMonitor.getThreadCount());
        CraftoMod.LOGGER.info("System Load Level: {}", systemMonitor.getLoadLevel());
        
        // Топ команд по времени выполнения
        commandExecutionTimes.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> CraftoMod.LOGGER.info("Command '{}': {}ms", 
                                                 entry.getKey(), entry.getValue()));
    }
    
    // Настройки оптимизации
    public void setAdaptiveOptimization(boolean enabled) {
        this.adaptiveOptimization = enabled;
        CraftoMod.LOGGER.info("Adaptive optimization: " + (enabled ? "enabled" : "disabled"));
    }
    
    public void setMaxConcurrentRequests(int max) {
        this.maxConcurrentRequests = Math.max(1, Math.min(10, max));
        CraftoMod.LOGGER.info("Max concurrent requests set to: " + this.maxConcurrentRequests);
    }
    
    public void setCacheExpirationTime(long timeMs) {
        this.cacheExpirationTime = Math.max(60000L, timeMs); // Минимум 1 минута
        CraftoMod.LOGGER.info("Cache expiration time set to: {}ms", this.cacheExpirationTime);
    }
    
    // Экспорт статистики
    public Map<String, Object> exportStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", totalRequests.get());
        stats.put("cacheHits", cacheHits.get());
        stats.put("averageResponseTime", averageResponseTime.get());
        stats.put("activeAgents", agentMemories.size());
        stats.put("maxConcurrentRequests", maxConcurrentRequests);
        stats.put("cacheExpirationTime", cacheExpirationTime);
        stats.put("commandExecutionTimes", new HashMap<>(commandExecutionTimes));
        stats.put("commandSuccessRates", new HashMap<>(commandSuccessRates));
        
        return stats;
    }
    
    public void shutdown() {
        requestBatcher.shutdown();
        systemMonitor.shutdown();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        CraftoMod.LOGGER.info("Performance manager shutdown completed");
    }
}