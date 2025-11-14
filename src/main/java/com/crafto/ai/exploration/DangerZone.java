package com.crafto.ai.exploration;

import java.time.LocalDateTime;

/**
 * Информация об опасной зоне
 */
public class DangerZone {
    private ChunkCoordinate coordinate;
    private DangerLevel dangerLevel;
    private String description;
    private LocalDateTime detectionTime;
    private String detectedBy;
    private boolean active;
    private LocalDateTime lastUpdate;
    
    // Конструктор по умолчанию для JSON десериализации
    public DangerZone() {
    }
    
    public DangerZone(ChunkCoordinate coordinate, DangerLevel dangerLevel, String description, LocalDateTime detectionTime) {
        this.coordinate = coordinate;
        this.dangerLevel = dangerLevel;
        this.description = description;
        this.detectionTime = detectionTime;
        this.active = true;
        this.lastUpdate = detectionTime;
    }
    
    /**
     * Обновляет информацию об опасности
     */
    public void updateDanger(DangerLevel newLevel, String newDescription) {
        this.dangerLevel = newLevel;
        this.description = newDescription;
        this.lastUpdate = LocalDateTime.now();
    }
    
    /**
     * Деактивирует опасную зону (например, после устранения угрозы)
     */
    public void deactivate(String reason) {
        this.active = false;
        this.description += " (Деактивировано: " + reason + ")";
        this.lastUpdate = LocalDateTime.now();
    }
    
    /**
     * Проверяет, требует ли зона обновления информации
     */
    public boolean needsUpdate() {
        // Обновляем информацию каждые 24 часа для активных зон
        return active && lastUpdate.isBefore(LocalDateTime.now().minusHours(24));
    }
    
    /**
     * Получает рекомендации по безопасности для этой зоны
     */
    public String getSafetyRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        
        int level = dangerLevel.getLevel();
        String threat = dangerLevel.getPrimaryThreat();
        
        if (level >= 8) {
            recommendations.append("КРИТИЧЕСКАЯ ОПАСНОСТЬ! Избегать любой ценой. ");
        } else if (level >= 6) {
            recommendations.append("Высокая опасность. Входить только с полной экипировкой. ");
        } else if (level >= 4) {
            recommendations.append("Умеренная опасность. Рекомендуется осторожность. ");
        } else {
            recommendations.append("Низкая опасность. Стандартные меры предосторожности. ");
        }
        
        // Специфичные рекомендации по типу угрозы
        switch (threat.toLowerCase()) {
            case "лава":
                recommendations.append("Взять огнестойкие зелья и ведра с водой. ");
                break;
            case "спавнеры мобов":
                recommendations.append("Подготовить оружие и броню. Взять факелы. ");
                break;
            case "опасный биом":
                recommendations.append("Изучить специфичные опасности биома. ");
                break;
            default:
                recommendations.append("Быть готовым к неожиданностям. ");
        }
        
        return recommendations.toString();
    }
    
    // Геттеры и сеттеры
    
    public ChunkCoordinate getCoordinate() {
        return coordinate;
    }
    
    public void setCoordinate(ChunkCoordinate coordinate) {
        this.coordinate = coordinate;
    }
    
    public DangerLevel getDangerLevel() {
        return dangerLevel;
    }
    
    public void setDangerLevel(DangerLevel dangerLevel) {
        this.dangerLevel = dangerLevel;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getDetectionTime() {
        return detectionTime;
    }
    
    public void setDetectionTime(LocalDateTime detectionTime) {
        this.detectionTime = detectionTime;
    }
    
    public String getDetectedBy() {
        return detectedBy;
    }
    
    public void setDetectedBy(String detectedBy) {
        this.detectedBy = detectedBy;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }
    
    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
    
    @Override
    public String toString() {
        return String.format("DangerZone{coordinate=%s, level=%d, threat='%s', active=%s}", 
                           coordinate, dangerLevel.getLevel(), dangerLevel.getPrimaryThreat(), active);
    }
}

/**
 * Уровень опасности
 */
class DangerLevel {
    private int level; // 1-10
    private String primaryThreat;
    private int dangerScore;
    
    // Конструктор по умолчанию для JSON десериализации
    public DangerLevel() {
    }
    
    public DangerLevel(int level, String primaryThreat, int dangerScore) {
        this.level = Math.max(1, Math.min(10, level));
        this.primaryThreat = primaryThreat;
        this.dangerScore = dangerScore;
    }
    
    /**
     * Получает текстовое описание уровня опасности
     */
    public String getLevelDescription() {
        if (level >= 9) return "КРИТИЧЕСКИЙ";
        if (level >= 7) return "ОЧЕНЬ ВЫСОКИЙ";
        if (level >= 5) return "ВЫСОКИЙ";
        if (level >= 3) return "СРЕДНИЙ";
        return "НИЗКИЙ";
    }
    
    /**
     * Получает цвет для отображения уровня опасности
     */
    public String getColor() {
        if (level >= 8) return "RED";
        if (level >= 6) return "ORANGE";
        if (level >= 4) return "YELLOW";
        if (level >= 2) return "GREEN";
        return "BLUE";
    }
    
    // Геттеры и сеттеры
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(10, level));
    }
    
    public String getPrimaryThreat() {
        return primaryThreat;
    }
    
    public void setPrimaryThreat(String primaryThreat) {
        this.primaryThreat = primaryThreat;
    }
    
    public int getDangerScore() {
        return dangerScore;
    }
    
    public void setDangerScore(int dangerScore) {
        this.dangerScore = dangerScore;
    }
    
    @Override
    public String toString() {
        return String.format("DangerLevel{level=%d (%s), threat='%s'}", 
                           level, getLevelDescription(), primaryThreat);
    }
}