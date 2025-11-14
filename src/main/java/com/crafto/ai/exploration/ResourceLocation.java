package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import java.time.LocalDateTime;

/**
 * Информация о местоположении ресурса
 */
public class ResourceLocation {
    private BlockPos position;
    private String resourceType;
    private int value;
    private LocalDateTime discoveryTime;
    private boolean extracted;
    private String discoveredBy;
    private int estimatedQuantity;
    private String accessDifficulty;
    
    // Конструктор по умолчанию для JSON десериализации
    public ResourceLocation() {
    }
    
    public ResourceLocation(BlockPos position, String resourceType, int value, LocalDateTime discoveryTime) {
        this.position = position;
        this.resourceType = resourceType;
        this.value = value;
        this.discoveryTime = discoveryTime;
        this.extracted = false;
        this.estimatedQuantity = 1;
        this.accessDifficulty = calculateAccessDifficulty();
    }
    
    /**
     * Вычисляет сложность доступа к ресурсу
     */
    private String calculateAccessDifficulty() {
        if (position == null) return "UNKNOWN";
        
        int y = position.getY();
        
        if (y < 0) {
            return "VERY_HARD"; // Глубокие пещеры
        } else if (y < 32) {
            return "HARD"; // Подземелья
        } else if (y < 64) {
            return "MEDIUM"; // Средний уровень
        } else {
            return "EASY"; // Поверхность
        }
    }
    
    /**
     * Получает приоритет добычи ресурса
     */
    public int getMiningPriority() {
        int priority = value;
        
        // Корректируем приоритет на основе сложности доступа
        switch (accessDifficulty) {
            case "EASY":
                priority += 50;
                break;
            case "MEDIUM":
                priority += 20;
                break;
            case "HARD":
                priority -= 10;
                break;
            case "VERY_HARD":
                priority -= 30;
                break;
        }
        
        // Снижаем приоритет для уже добытых ресурсов
        if (extracted) {
            priority = 0;
        }
        
        return Math.max(0, priority);
    }
    
    /**
     * Получает расстояние до ресурса от указанной позиции
     */
    public double getDistanceFrom(BlockPos fromPos) {
        if (position == null || fromPos == null) return Double.MAX_VALUE;
        
        double dx = position.getX() - fromPos.getX();
        double dy = position.getY() - fromPos.getY();
        double dz = position.getZ() - fromPos.getZ();
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Проверяет, является ли ресурс редким
     */
    public boolean isRare() {
        return resourceType.contains("diamond") || 
               resourceType.contains("emerald") || 
               resourceType.contains("ancient_debris") ||
               resourceType.contains("spawner");
    }
    
    /**
     * Получает категорию ресурса
     */
    public String getCategory() {
        if (resourceType.contains("diamond")) return "PRECIOUS";
        if (resourceType.contains("emerald")) return "PRECIOUS";
        if (resourceType.contains("ancient_debris")) return "PRECIOUS";
        if (resourceType.contains("gold")) return "VALUABLE";
        if (resourceType.contains("iron")) return "COMMON";
        if (resourceType.contains("copper")) return "COMMON";
        if (resourceType.contains("coal")) return "FUEL";
        if (resourceType.contains("redstone")) return "TECHNICAL";
        if (resourceType.contains("lapis")) return "ENCHANTING";
        if (resourceType.contains("spawner")) return "SPECIAL";
        return "OTHER";
    }
    
    /**
     * Отмечает ресурс как добытый
     */
    public void markAsExtracted(String extractedBy) {
        this.extracted = true;
        this.discoveredBy = extractedBy;
    }
    
    // Геттеры и сеттеры
    
    public BlockPos getPosition() {
        return position;
    }
    
    public void setPosition(BlockPos position) {
        this.position = position;
        this.accessDifficulty = calculateAccessDifficulty();
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    
    public int getValue() {
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    public LocalDateTime getDiscoveryTime() {
        return discoveryTime;
    }
    
    public void setDiscoveryTime(LocalDateTime discoveryTime) {
        this.discoveryTime = discoveryTime;
    }
    
    public boolean isExtracted() {
        return extracted;
    }
    
    public void setExtracted(boolean extracted) {
        this.extracted = extracted;
    }
    
    public String getDiscoveredBy() {
        return discoveredBy;
    }
    
    public void setDiscoveredBy(String discoveredBy) {
        this.discoveredBy = discoveredBy;
    }
    
    public int getEstimatedQuantity() {
        return estimatedQuantity;
    }
    
    public void setEstimatedQuantity(int estimatedQuantity) {
        this.estimatedQuantity = estimatedQuantity;
    }
    
    public String getAccessDifficulty() {
        return accessDifficulty;
    }
    
    public void setAccessDifficulty(String accessDifficulty) {
        this.accessDifficulty = accessDifficulty;
    }
    
    @Override
    public String toString() {
        return String.format("ResourceLocation{type='%s', pos=%s, value=%d, difficulty='%s', extracted=%s}", 
                           resourceType, position, value, accessDifficulty, extracted);
    }
}