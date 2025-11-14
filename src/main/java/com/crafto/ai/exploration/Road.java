package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Дорога между путевыми точками
 */
public class Road {
    private String roadId;
    private BlockPos startPosition;
    private BlockPos endPosition;
    private String startWaypoint;
    private String endWaypoint;
    private RoadType roadType;
    private double distance;
    private List<BlockPos> waypoints;
    private LocalDateTime creationTime;
    private LocalDateTime lastUsed;
    private String createdBy;
    private boolean active;
    private int usageCount;
    private String condition; // GOOD, FAIR, POOR, BLOCKED
    private boolean isBidirectional;
    
    // Конструктор по умолчанию для JSON десериализации
    public Road() {
        this.waypoints = new ArrayList<>();
    }
    
    public Road(String roadId, BlockPos startPosition, BlockPos endPosition, RoadType roadType) {
        this();
        this.roadId = roadId;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.roadType = roadType;
        this.creationTime = LocalDateTime.now();
        this.active = true;
        this.usageCount = 0;
        this.condition = "GOOD";
        this.isBidirectional = true;
        calculateDistance();
    }
    
    /**
     * Вычисляет расстояние дороги
     */
    private void calculateDistance() {
        if (startPosition == null || endPosition == null) {
            distance = 0.0;
            return;
        }
        
        double totalDistance = 0.0;
        BlockPos current = startPosition;
        
        // Добавляем расстояния через промежуточные точки
        for (BlockPos waypoint : waypoints) {
            totalDistance += calculateBlockDistance(current, waypoint);
            current = waypoint;
        }
        
        // Добавляем расстояние до конечной точки
        totalDistance += calculateBlockDistance(current, endPosition);
        
        this.distance = totalDistance;
    }
    
    /**
     * Вычисляет расстояние между двумя блоками
     */
    private double calculateBlockDistance(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Добавляет промежуточную точку к дороге
     */
    public void addWaypoint(BlockPos waypoint) {
        waypoints.add(waypoint);
        calculateDistance();
    }
    
    /**
     * Отмечает использование дороги
     */
    public void markUsed() {
        this.lastUsed = LocalDateTime.now();
        this.usageCount++;
    }
    
    /**
     * Получает другую путевую точку дороги
     */
    public String getOtherWaypoint(String currentWaypoint) {
        if (startWaypoint != null && startWaypoint.equals(currentWaypoint)) {
            return endWaypoint;
        } else if (endWaypoint != null && endWaypoint.equals(currentWaypoint)) {
            return startWaypoint;
        }
        return null;
    }
    
    /**
     * Получает скорость передвижения по дороге
     */
    public double getTravelSpeed() {
        double baseSpeed = 4.3; // Базовая скорость ходьбы в блоках/сек
        
        // Модификаторы скорости в зависимости от типа дороги
        double speedMultiplier = roadType.getSpeedMultiplier();
        
        // Модификаторы в зависимости от состояния дороги
        switch (condition) {
            case "GOOD":
                speedMultiplier *= 1.0;
                break;
            case "FAIR":
                speedMultiplier *= 0.9;
                break;
            case "POOR":
                speedMultiplier *= 0.7;
                break;
            case "BLOCKED":
                speedMultiplier *= 0.3;
                break;
        }
        
        return baseSpeed * speedMultiplier;
    }
    
    /**
     * Получает время путешествия в секундах
     */
    public int getTravelTime() {
        double speed = getTravelSpeed();
        return (int) (distance / speed);
    }
    
    /**
     * Проверяет, нуждается ли дорога в ремонте
     */
    public boolean needsMaintenance() {
        return "POOR".equals(condition) || "BLOCKED".equals(condition);
    }
    
    /**
     * Получает приоритет дороги для навигации
     */
    public int getNavigationPriority() {
        int priority = roadType.getPriority();
        
        // Бонус за частое использование
        priority += Math.min(usageCount * 2, 100);
        
        // Штраф за плохое состояние
        switch (condition) {
            case "FAIR":
                priority -= 10;
                break;
            case "POOR":
                priority -= 30;
                break;
            case "BLOCKED":
                priority -= 100;
                break;
        }
        
        // Штраф за длину (предпочитаем короткие дороги)
        priority -= (int) (distance / 100);
        
        return Math.max(0, priority);
    }
    
    /**
     * Получает описание дороги
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Дорога ").append(roadId);
        desc.append(" (").append(roadType.getDisplayName()).append(")");
        desc.append(" от ").append(startWaypoint != null ? startWaypoint : "неизвестно");
        desc.append(" до ").append(endWaypoint != null ? endWaypoint : "неизвестно");
        desc.append("\nДлина: ").append(String.format("%.1f", distance)).append(" блоков");
        desc.append("\nВремя в пути: ").append(formatTime(getTravelTime()));
        desc.append("\nСостояние: ").append(getConditionDescription());
        desc.append("\nИспользований: ").append(usageCount);
        
        return desc.toString();
    }
    
    /**
     * Получает описание состояния дороги
     */
    private String getConditionDescription() {
        switch (condition) {
            case "GOOD": return "Хорошее";
            case "FAIR": return "Удовлетворительное";
            case "POOR": return "Плохое";
            case "BLOCKED": return "Заблокировано";
            default: return "Неизвестно";
        }
    }
    
    /**
     * Форматирует время для отображения
     */
    private String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + " сек";
        } else if (seconds < 3600) {
            return (seconds / 60) + " мин " + (seconds % 60) + " сек";
        } else {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            return hours + " ч " + minutes + " мин";
        }
    }
    
    // Геттеры и сеттеры
    
    public String getRoadId() {
        return roadId;
    }
    
    public void setRoadId(String roadId) {
        this.roadId = roadId;
    }
    
    public BlockPos getStartPosition() {
        return startPosition;
    }
    
    public void setStartPosition(BlockPos startPosition) {
        this.startPosition = startPosition;
        calculateDistance();
    }
    
    public BlockPos getEndPosition() {
        return endPosition;
    }
    
    public void setEndPosition(BlockPos endPosition) {
        this.endPosition = endPosition;
        calculateDistance();
    }
    
    public String getStartWaypoint() {
        return startWaypoint;
    }
    
    public void setStartWaypoint(String startWaypoint) {
        this.startWaypoint = startWaypoint;
    }
    
    public String getEndWaypoint() {
        return endWaypoint;
    }
    
    public void setEndWaypoint(String endWaypoint) {
        this.endWaypoint = endWaypoint;
    }
    
    public RoadType getRoadType() {
        return roadType;
    }
    
    public void setRoadType(RoadType roadType) {
        this.roadType = roadType;
    }
    
    public double getDistance() {
        return distance;
    }
    
    public List<BlockPos> getWaypoints() {
        return waypoints;
    }
    
    public void setWaypoints(List<BlockPos> waypoints) {
        this.waypoints = waypoints;
        calculateDistance();
    }
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }
    
    public LocalDateTime getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public boolean isBidirectional() {
        return isBidirectional;
    }
    
    public void setBidirectional(boolean bidirectional) {
        isBidirectional = bidirectional;
    }
    
    @Override
    public String toString() {
        return String.format("Road{id='%s', type=%s, distance=%.1f, condition='%s'}", 
                           roadId, roadType, distance, condition);
    }
}

