package com.crafto.ai.exploration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Чанк карты с информацией о местности
 */
public class MapChunk {
    private ChunkCoordinate coordinate;
    private String biome;
    private String terrainType;
    private Map<String, Integer> blockComposition;
    private LocalDateTime lastUpdated;
    private boolean hasWater;
    private boolean hasLava;
    private boolean hasCaves;
    private boolean hasStructures;
    private int averageHeight;
    private String exploredBy;
    private double dangerLevel;
    private String notes;
    
    // Конструктор по умолчанию для JSON десериализации
    public MapChunk() {
        this.blockComposition = new HashMap<>();
    }
    
    public MapChunk(ChunkCoordinate coordinate) {
        this();
        this.coordinate = coordinate;
        this.lastUpdated = LocalDateTime.now();
        this.dangerLevel = 0.0;
    }
    
    /**
     * Обновляет чанк карты на основе исследованной области
     */
    public void updateFromExploredArea(ExploredArea exploredArea) {
        this.biome = exploredArea.getBiome();
        this.blockComposition = new HashMap<>(exploredArea.getBlockCounts());
        this.hasWater = exploredArea.isHasWater();
        this.hasLava = exploredArea.isHasLava();
        this.hasCaves = exploredArea.getCaveCount() > 0;
        this.hasStructures = !exploredArea.getStructures().isEmpty();
        this.exploredBy = exploredArea.getExploredBy();
        this.lastUpdated = LocalDateTime.now();
        
        // Определяем тип местности
        determineTerrainType();
        
        // Вычисляем средний уровень высоты
        calculateAverageHeight();
    }
    
    /**
     * Определяет тип местности на основе состава блоков
     */
    private void determineTerrainType() {
        int waterBlocks = blockComposition.getOrDefault("minecraft:water", 0);
        int stoneBlocks = blockComposition.getOrDefault("minecraft:stone", 0) + 
                         blockComposition.getOrDefault("minecraft:deepslate", 0);
        int dirtBlocks = blockComposition.getOrDefault("minecraft:dirt", 0) + 
                        blockComposition.getOrDefault("minecraft:grass_block", 0);
        int sandBlocks = blockComposition.getOrDefault("minecraft:sand", 0);
        int snowBlocks = blockComposition.getOrDefault("minecraft:snow", 0) + 
                        blockComposition.getOrDefault("minecraft:ice", 0);
        
        if (waterBlocks > 1000) {
            terrainType = "WATER";
        } else if (stoneBlocks > 2000) {
            terrainType = "MOUNTAIN";
        } else if (sandBlocks > 1000) {
            terrainType = "DESERT";
        } else if (snowBlocks > 500) {
            terrainType = "SNOW";
        } else if (dirtBlocks > 1000) {
            terrainType = "PLAINS";
        } else {
            terrainType = "MIXED";
        }
    }
    
    /**
     * Вычисляет средний уровень высоты (упрощенная версия)
     */
    private void calculateAverageHeight() {
        // Простая эвристика на основе состава блоков
        int surfaceBlocks = blockComposition.getOrDefault("minecraft:grass_block", 0) + 
                           blockComposition.getOrDefault("minecraft:sand", 0) + 
                           blockComposition.getOrDefault("minecraft:snow", 0);
        
        if (surfaceBlocks > 0) {
            // Предполагаем, что большинство поверхностных блоков на уровне 60-80
            averageHeight = 70;
        } else {
            // Если нет поверхностных блоков, вероятно подземелье
            averageHeight = 30;
        }
    }
    
    /**
     * Получает краткое описание чанка
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Чанк ").append(coordinate);
        desc.append(" (").append(biome != null ? biome : "Неизвестный биом").append(")");
        desc.append(" - ").append(terrainType != null ? getTerrainTypeDescription() : "Смешанная местность");
        
        if (hasWater) desc.append(", Вода");
        if (hasLava) desc.append(", Лава");
        if (hasCaves) desc.append(", Пещеры");
        if (hasStructures) desc.append(", Структуры");
        
        return desc.toString();
    }
    
    /**
     * Получает описание типа местности
     */
    private String getTerrainTypeDescription() {
        switch (terrainType) {
            case "WATER": return "Водная";
            case "MOUNTAIN": return "Горная";
            case "DESERT": return "Пустынная";
            case "SNOW": return "Снежная";
            case "PLAINS": return "Равнинная";
            case "MIXED": return "Смешанная";
            default: return "Неизвестная";
        }
    }
    
    /**
     * Проверяет, подходит ли чанк для строительства
     */
    public boolean isSuitableForBuilding() {
        return !hasLava && 
               !hasCaves && 
               dangerLevel < 5.0 && 
               ("PLAINS".equals(terrainType) || "MIXED".equals(terrainType));
    }
    
    /**
     * Проверяет, содержит ли чанк ценные ресурсы
     */
    public boolean hasValuableResources() {
        return blockComposition.containsKey("minecraft:diamond_ore") ||
               blockComposition.containsKey("minecraft:emerald_ore") ||
               blockComposition.containsKey("minecraft:ancient_debris") ||
               blockComposition.containsKey("minecraft:gold_ore");
    }
    
    /**
     * Получает оценку ценности чанка
     */
    public int getValueScore() {
        int score = 0;
        
        // Бонус за ценные ресурсы
        score += blockComposition.getOrDefault("minecraft:diamond_ore", 0) * 100;
        score += blockComposition.getOrDefault("minecraft:emerald_ore", 0) * 90;
        score += blockComposition.getOrDefault("minecraft:ancient_debris", 0) * 150;
        score += blockComposition.getOrDefault("minecraft:gold_ore", 0) * 60;
        score += blockComposition.getOrDefault("minecraft:iron_ore", 0) * 40;
        
        // Бонус за структуры
        if (hasStructures) {
            score += 200;
        }
        
        // Штраф за опасности
        if (hasLava) {
            score -= 100;
        }
        if (dangerLevel > 5) {
            score -= (int) (dangerLevel * 20);
        }
        
        return Math.max(0, score);
    }
    
    // Геттеры и сеттеры
    
    public ChunkCoordinate getCoordinate() {
        return coordinate;
    }
    
    public void setCoordinate(ChunkCoordinate coordinate) {
        this.coordinate = coordinate;
    }
    
    public String getBiome() {
        return biome;
    }
    
    public void setBiome(String biome) {
        this.biome = biome;
    }
    
    public String getTerrainType() {
        return terrainType;
    }
    
    public void setTerrainType(String terrainType) {
        this.terrainType = terrainType;
    }
    
    public Map<String, Integer> getBlockComposition() {
        return blockComposition;
    }
    
    public void setBlockComposition(Map<String, Integer> blockComposition) {
        this.blockComposition = blockComposition;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public boolean isHasWater() {
        return hasWater;
    }
    
    public void setHasWater(boolean hasWater) {
        this.hasWater = hasWater;
    }
    
    public boolean isHasLava() {
        return hasLava;
    }
    
    public void setHasLava(boolean hasLava) {
        this.hasLava = hasLava;
    }
    
    public boolean isHasCaves() {
        return hasCaves;
    }
    
    public void setHasCaves(boolean hasCaves) {
        this.hasCaves = hasCaves;
    }
    
    public boolean isHasStructures() {
        return hasStructures;
    }
    
    public void setHasStructures(boolean hasStructures) {
        this.hasStructures = hasStructures;
    }
    
    public int getAverageHeight() {
        return averageHeight;
    }
    
    public void setAverageHeight(int averageHeight) {
        this.averageHeight = averageHeight;
    }
    
    public String getExploredBy() {
        return exploredBy;
    }
    
    public void setExploredBy(String exploredBy) {
        this.exploredBy = exploredBy;
    }
    
    public double getDangerLevel() {
        return dangerLevel;
    }
    
    public void setDangerLevel(double dangerLevel) {
        this.dangerLevel = dangerLevel;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public String toString() {
        return String.format("MapChunk{coord=%s, biome='%s', terrain='%s', value=%d}", 
                           coordinate, biome, terrainType, getValueScore());
    }
}