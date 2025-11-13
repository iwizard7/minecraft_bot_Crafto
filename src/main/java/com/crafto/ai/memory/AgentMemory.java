package com.crafto.ai.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AgentMemory {
    private static final Gson GSON = new Gson();
    private static final String MEMORY_DIR = "config/crafto_ai/memory/";
    
    private final String agentName;
    private final Map<String, SuccessfulStrategy> strategies = new ConcurrentHashMap<>();
    private final Map<String, PlayerInteraction> playerInteractions = new ConcurrentHashMap<>();
    private final Map<String, LocationInfo> knownLocations = new ConcurrentHashMap<>();
    private final Map<String, Long> commandHistory = new ConcurrentHashMap<>();
    
    public AgentMemory(String agentName) {
        this.agentName = agentName;
        loadMemory();
    }
    
    // Стратегии выполнения задач
    public static class SuccessfulStrategy {
        public String taskType;
        public String strategy;
        public int successCount;
        public long averageTime;
        public double successRate;
        public long lastUsed;
        
        public SuccessfulStrategy(String taskType, String strategy) {
            this.taskType = taskType;
            this.strategy = strategy;
            this.successCount = 0;
            this.averageTime = 0;
            this.successRate = 0.0;
            this.lastUsed = System.currentTimeMillis();
        }
    }
    
    // Взаимодействия с игроками
    public static class PlayerInteraction {
        public String playerName;
        public List<String> preferredCommands;
        public Map<String, Integer> commandFrequency;
        public long lastInteraction;
        public String preferredCommunicationStyle;
        
        public PlayerInteraction(String playerName) {
            this.playerName = playerName;
            this.preferredCommands = new ArrayList<>();
            this.commandFrequency = new HashMap<>();
            this.lastInteraction = System.currentTimeMillis();
            this.preferredCommunicationStyle = "normal";
        }
    }
    
    // Информация о локациях
    public static class LocationInfo {
        public BlockPos position;
        public String biome;
        public List<EntityType<?>> commonMobs;
        public Map<String, Integer> resourceDensity;
        public boolean isExplored;
        public long lastVisited;
        public double dangerLevel;
        
        public LocationInfo(BlockPos position, String biome) {
            this.position = position;
            this.biome = biome;
            this.commonMobs = new ArrayList<>();
            this.resourceDensity = new HashMap<>();
            this.isExplored = false;
            this.lastVisited = System.currentTimeMillis();
            this.dangerLevel = 0.0;
        }
    }
    
    // Запоминание успешной стратегии
    public void recordSuccessfulStrategy(String taskType, String strategy, long executionTime, boolean success) {
        String key = taskType + "_" + strategy.hashCode();
        SuccessfulStrategy existing = strategies.get(key);
        
        if (existing == null) {
            existing = new SuccessfulStrategy(taskType, strategy);
            strategies.put(key, existing);
        }
        
        if (success) {
            existing.successCount++;
            existing.averageTime = (existing.averageTime + executionTime) / 2;
        }
        
        existing.lastUsed = System.currentTimeMillis();
        existing.successRate = calculateSuccessRate(taskType);
        
        saveMemory();
    }
    
    // Получение лучшей стратегии для задачи
    public Optional<SuccessfulStrategy> getBestStrategy(String taskType) {
        return strategies.values().stream()
                .filter(s -> s.taskType.equals(taskType))
                .max(Comparator.comparing(s -> s.successRate * s.successCount));
    }
    
    // Запоминание взаимодействия с игроком
    public void recordPlayerInteraction(String playerName, String command) {
        PlayerInteraction interaction = playerInteractions.computeIfAbsent(
            playerName, PlayerInteraction::new);
        
        interaction.commandFrequency.merge(command, 1, Integer::sum);
        interaction.lastInteraction = System.currentTimeMillis();
        
        // Обновляем предпочитаемые команды
        updatePreferredCommands(interaction);
        
        saveMemory();
    }
    
    // Получение предпочтений игрока
    public Optional<PlayerInteraction> getPlayerPreferences(String playerName) {
        return Optional.ofNullable(playerInteractions.get(playerName));
    }
    
    // Запоминание информации о локации
    public void recordLocation(BlockPos pos, String biome, List<EntityType<?>> mobs) {
        String key = pos.getX() + "_" + pos.getZ();
        LocationInfo location = knownLocations.computeIfAbsent(
            key, k -> new LocationInfo(pos, biome));
        
        location.commonMobs.clear();
        location.commonMobs.addAll(mobs);
        location.lastVisited = System.currentTimeMillis();
        location.isExplored = true;
        
        saveMemory();
    }
    
    // Получение информации о локации
    public Optional<LocationInfo> getLocationInfo(BlockPos pos) {
        String key = pos.getX() + "_" + pos.getZ();
        return Optional.ofNullable(knownLocations.get(key));
    }
    
    // Получение всех изученных локаций
    public List<LocationInfo> getExploredLocations() {
        return knownLocations.values().stream()
                .filter(loc -> loc.isExplored)
                .sorted(Comparator.comparing(loc -> -loc.lastVisited))
                .toList();
    }
    
    // Запоминание времени выполнения команды
    public void recordCommandTime(String command, long executionTime) {
        commandHistory.put(command, executionTime);
        saveMemory();
    }
    
    // Получение среднего времени выполнения команды
    public long getAverageCommandTime(String command) {
        return commandHistory.getOrDefault(command, 5000L); // 5 секунд по умолчанию
    }
    
    // Очистка старых данных
    public void cleanupOldData() {
        long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L); // 7 дней
        
        strategies.entrySet().removeIf(entry -> entry.getValue().lastUsed < cutoffTime);
        playerInteractions.entrySet().removeIf(entry -> entry.getValue().lastInteraction < cutoffTime);
        knownLocations.entrySet().removeIf(entry -> entry.getValue().lastVisited < cutoffTime);
        
        saveMemory();
    }
    
    private double calculateSuccessRate(String taskType) {
        List<SuccessfulStrategy> taskStrategies = strategies.values().stream()
                .filter(s -> s.taskType.equals(taskType))
                .toList();
        
        if (taskStrategies.isEmpty()) return 0.0;
        
        int totalAttempts = taskStrategies.stream().mapToInt(s -> s.successCount).sum();
        return totalAttempts > 0 ? (double) taskStrategies.size() / totalAttempts : 0.0;
    }
    
    private void updatePreferredCommands(PlayerInteraction interaction) {
        interaction.preferredCommands = interaction.commandFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }
    
    // Сохранение памяти в файл
    private void saveMemory() {
        try {
            File memoryDir = new File(MEMORY_DIR);
            if (!memoryDir.exists()) {
                memoryDir.mkdirs();
            }
            
            MemoryData data = new MemoryData();
            data.strategies = strategies;
            data.playerInteractions = playerInteractions;
            data.knownLocations = knownLocations;
            data.commandHistory = commandHistory;
            
            try (FileWriter writer = new FileWriter(MEMORY_DIR + agentName + "_memory.json")) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save agent memory: " + e.getMessage());
        }
    }
    
    // Загрузка памяти из файла
    private void loadMemory() {
        try {
            File memoryFile = new File(MEMORY_DIR + agentName + "_memory.json");
            if (!memoryFile.exists()) {
                return;
            }
            
            try (FileReader reader = new FileReader(memoryFile)) {
                Type type = new TypeToken<MemoryData>(){}.getType();
                MemoryData data = GSON.fromJson(reader, type);
                
                if (data != null) {
                    if (data.strategies != null) strategies.putAll(data.strategies);
                    if (data.playerInteractions != null) playerInteractions.putAll(data.playerInteractions);
                    if (data.knownLocations != null) knownLocations.putAll(data.knownLocations);
                    if (data.commandHistory != null) commandHistory.putAll(data.commandHistory);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load agent memory: " + e.getMessage());
        }
    }
    
    private static class MemoryData {
        Map<String, SuccessfulStrategy> strategies;
        Map<String, PlayerInteraction> playerInteractions;
        Map<String, LocationInfo> knownLocations;
        Map<String, Long> commandHistory;
    }
}