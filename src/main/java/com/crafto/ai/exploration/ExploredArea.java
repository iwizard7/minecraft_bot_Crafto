package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Информация об исследованной области
 */
public class ExploredArea {
    private ChunkCoordinate coordinate;
    private String biome;
    private Map<String, Integer> blockCounts;
    private List<ResourceLocation> resources;
    private Map<String, BlockPos> structures;
    private LocalDateTime explorationTime;
    private String exploredBy;
    private double averageHeight;
    private boolean hasWater;
    private boolean hasLava;
    private int caveCount;
    
    // Конструктор по умолчанию для JSON десериализации
    public ExploredArea() {
        this.blockCounts = new HashMap<>();
        this.resources = new ArrayList<>();
        this.structures = new HashMap<>();
    }
    
    public ExploredArea(ChunkCoordinate coordinate) {
        this();
        this.coordinate = coordinate;
        this.explorationTime = LocalDateTime.now();
    }
    
    /**
     * Добавляет количество блоков к общей статистике
     */
    public void addBlockCounts(Map<String, Integer> counts) {
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            blockCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        
        // Обновляем дополнительную информацию
        updateAreaInfo();
    }
    
    /**
     * Добавляет найденные ресурсы
     */
    public void addResources(List<ResourceLocation> newResources) {
        resources.addAll(newResources);
    }
    
    /**
     * Добавляет структуру
     */
    public void addStructure(String structureType, BlockPos position) {
        structures.put(structureType, position);
    }
    
    /**
     * Обновляет дополнительную информацию об области
     */
    private void updateAreaInfo() {
        // Проверяем наличие воды и лавы
        hasWater = blockCounts.containsKey("minecraft:water") || 
                   blockCounts.containsKey("minecraft:flowing_water");
        hasLava = blockCounts.containsKey("minecraft:lava") || 
                  blockCounts.containsKey("minecraft:flowing_lava");
        
        // Подсчитываем пещеры (примерная оценка по количеству воздуха)
        int airBlocks = blockCounts.getOrDefault("minecraft:air", 0) + 
                       blockCounts.getOrDefault("minecraft:cave_air", 0);
        caveCount = airBlocks / 100; // Примерная оценка
    }
    
    /**
     * Получает общую ценность области на основе найденных ресурсов
     */
    public int getTotalValue() {
        return resources.stream().mapToInt(ResourceLocation::getValue).sum();
    }
    
    /**
     * Получает плотность ресурсов (ресурсов на блок)
     */
    public double getResourceDensity() {
        int totalBlocks = blockCounts.values().stream().mapToInt(Integer::intValue).sum();
        return totalBlocks > 0 ? (double) resources.size() / totalBlocks : 0.0;
    }
    
    /**
     * Проверяет, подходит ли область для строительства
     */
    public boolean isSuitableForBuilding() {
        // Проверяем различные факторы
        boolean flatEnough = averageHeight > 0; // Будет вычислено позже
        boolean notTooMuchWater = !hasWater || blockCounts.getOrDefault("minecraft:water", 0) < 50;
        boolean noLava = !hasLava;
        boolean notTooCavy = caveCount < 5;
        
        return flatEnough && notTooMuchWater && noLava && notTooCavy;
    }
    
    /**
     * Получает краткое описание области
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Биом: ").append(biome != null ? biome : "Неизвестно");
        desc.append(", Ресурсы: ").append(resources.size());
        desc.append(", Ценность: ").append(getTotalValue());
        
        if (hasWater) desc.append(", Есть вода");
        if (hasLava) desc.append(", Есть лава");
        if (!structures.isEmpty()) desc.append(", Структуры: ").append(structures.size());
        
        return desc.toString();
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
    
    public Map<String, Integer> getBlockCounts() {
        return blockCounts;
    }
    
    public void setBlockCounts(Map<String, Integer> blockCounts) {
        this.blockCounts = blockCounts;
    }
    
    public List<ResourceLocation> getResources() {
        return resources;
    }
    
    public void setResources(List<ResourceLocation> resources) {
        this.resources = resources;
    }
    
    public Map<String, BlockPos> getStructures() {
        return structures;
    }
    
    public void setStructures(Map<String, BlockPos> structures) {
        this.structures = structures;
    }
    
    public LocalDateTime getExplorationTime() {
        return explorationTime;
    }
    
    public void setExplorationTime(LocalDateTime explorationTime) {
        this.explorationTime = explorationTime;
    }
    
    public String getExploredBy() {
        return exploredBy;
    }
    
    public void setExploredBy(String exploredBy) {
        this.exploredBy = exploredBy;
    }
    
    public double getAverageHeight() {
        return averageHeight;
    }
    
    public void setAverageHeight(double averageHeight) {
        this.averageHeight = averageHeight;
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
    
    public int getCaveCount() {
        return caveCount;
    }
    
    public void setCaveCount(int caveCount) {
        this.caveCount = caveCount;
    }
    
    @Override
    public String toString() {
        return String.format("ExploredArea{coordinate=%s, biome='%s', resources=%d, value=%d}", 
                           coordinate, biome, resources.size(), getTotalValue());
    }
}