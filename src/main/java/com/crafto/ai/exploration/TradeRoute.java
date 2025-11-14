package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Торговый маршрут между точками
 */
public class TradeRoute {
    private String routeId;
    private BlockPos startPoint;
    private BlockPos endPoint;
    private List<BlockPos> waypoints;
    private String routeType;
    private double distance;
    private int estimatedTravelTime;
    private List<String> tradableResources;
    private LocalDateTime creationTime;
    private LocalDateTime lastUsed;
    private int usageCount;
    private boolean active;
    private String createdBy;
    
    // Конструктор по умолчанию для JSON десериализации
    public TradeRoute() {
        this.waypoints = new ArrayList<>();
        this.tradableResources = new ArrayList<>();
    }
    
    public TradeRoute(String routeId, BlockPos startPoint, BlockPos endPoint) {
        this();
        this.routeId = routeId;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.creationTime = LocalDateTime.now();
        this.active = true;
        this.usageCount = 0;
        this.routeType = "STANDARD";
        calculateDistance();
        estimateTravelTime();
    }
    
    /**
     * Вычисляет расстояние маршрута
     */
    private void calculateDistance() {
        if (startPoint == null || endPoint == null) {
            distance = 0.0;
            return;
        }
        
        double totalDistance = 0.0;
        BlockPos current = startPoint;
        
        // Добавляем расстояния через промежуточные точки
        for (BlockPos waypoint : waypoints) {
            totalDistance += calculateBlockDistance(current, waypoint);
            current = waypoint;
        }
        
        // Добавляем расстояние до конечной точки
        totalDistance += calculateBlockDistance(current, endPoint);
        
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
     * Оценивает время путешествия в секундах
     */
    private void estimateTravelTime() {
        // Базовая скорость: 4.3 блока в секунду (ходьба)
        double baseSpeed = 4.3;
        
        // Корректировки скорости в зависимости от типа маршрута
        double speedMultiplier = 1.0;
        switch (routeType) {
            case "ROAD":
                speedMultiplier = 1.2; // Дороги быстрее
                break;
            case "WATER":
                speedMultiplier = 0.8; // Вода медленнее
                break;
            case "NETHER":
                speedMultiplier = 2.0; // Незер быстрее (соотношение 1:8)
                break;
            case "ELYTRA":
                speedMultiplier = 3.0; // Элитры очень быстро
                break;
        }
        
        this.estimatedTravelTime = (int) (distance / (baseSpeed * speedMultiplier));
    }
    
    /**
     * Добавляет промежуточную точку к маршруту
     */
    public void addWaypoint(BlockPos waypoint) {
        waypoints.add(waypoint);
        calculateDistance();
        estimateTravelTime();
    }
    
    /**
     * Добавляет ресурс, который можно торговать по этому маршруту
     */
    public void addTradableResource(String resource) {
        if (!tradableResources.contains(resource)) {
            tradableResources.add(resource);
        }
    }
    
    /**
     * Отмечает использование маршрута
     */
    public void markUsed() {
        this.lastUsed = LocalDateTime.now();
        this.usageCount++;
    }
    
    /**
     * Проверяет, является ли маршрут эффективным
     */
    public boolean isEfficient() {
        // Маршрут считается эффективным, если он используется регулярно
        if (lastUsed == null) return false;
        
        // Если использовался в последние 7 дней и имеет достаточно использований
        boolean recentlyUsed = lastUsed.isAfter(LocalDateTime.now().minusDays(7));
        boolean frequentlyUsed = usageCount > 5;
        
        return recentlyUsed && frequentlyUsed;
    }
    
    /**
     * Получает оценку полезности маршрута
     */
    public int getUtilityScore() {
        int score = 0;
        
        // Бонус за частоту использования
        score += usageCount * 10;
        
        // Бонус за недавнее использование
        if (lastUsed != null && lastUsed.isAfter(LocalDateTime.now().minusDays(3))) {
            score += 50;
        }
        
        // Бонус за количество торгуемых ресурсов
        score += tradableResources.size() * 20;
        
        // Штраф за длину маршрута (предпочитаем короткие маршруты)
        score -= (int) (distance / 100);
        
        // Бонус за специальные типы маршрутов
        switch (routeType) {
            case "NETHER":
                score += 100; // Незер маршруты очень ценны
                break;
            case "ELYTRA":
                score += 80;
                break;
            case "ROAD":
                score += 30;
                break;
        }
        
        return Math.max(0, score);
    }
    
    /**
     * Получает описание маршрута
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Маршрут ").append(routeId);
        desc.append(" от ").append(formatPosition(startPoint));
        desc.append(" до ").append(formatPosition(endPoint));
        desc.append(" (").append(String.format("%.1f", distance)).append(" блоков, ");
        desc.append(formatTime(estimatedTravelTime)).append(")");
        
        if (!tradableResources.isEmpty()) {
            desc.append("\nТоргуемые ресурсы: ").append(String.join(", ", tradableResources));
        }
        
        return desc.toString();
    }
    
    /**
     * Форматирует позицию для отображения
     */
    private String formatPosition(BlockPos pos) {
        if (pos == null) return "неизвестно";
        return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
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
    
    public String getRouteId() {
        return routeId;
    }
    
    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }
    
    public BlockPos getStartPoint() {
        return startPoint;
    }
    
    public void setStartPoint(BlockPos startPoint) {
        this.startPoint = startPoint;
        calculateDistance();
        estimateTravelTime();
    }
    
    public BlockPos getEndPoint() {
        return endPoint;
    }
    
    public void setEndPoint(BlockPos endPoint) {
        this.endPoint = endPoint;
        calculateDistance();
        estimateTravelTime();
    }
    
    public List<BlockPos> getWaypoints() {
        return waypoints;
    }
    
    public void setWaypoints(List<BlockPos> waypoints) {
        this.waypoints = waypoints;
        calculateDistance();
        estimateTravelTime();
    }
    
    public String getRouteType() {
        return routeType;
    }
    
    public void setRouteType(String routeType) {
        this.routeType = routeType;
        estimateTravelTime();
    }
    
    public double getDistance() {
        return distance;
    }
    
    public int getEstimatedTravelTime() {
        return estimatedTravelTime;
    }
    
    public List<String> getTradableResources() {
        return tradableResources;
    }
    
    public void setTradableResources(List<String> tradableResources) {
        this.tradableResources = tradableResources;
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
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    @Override
    public String toString() {
        return String.format("TradeRoute{id='%s', distance=%.1f, type='%s', active=%s}", 
                           routeId, distance, routeType, active);
    }
}