package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Путевая точка в навигационной системе
 */
public class Waypoint {
    private String name;
    private BlockPos position;
    private WaypointType type;
    private String description;
    private LocalDateTime creationTime;
    private LocalDateTime lastVisited;
    private String createdBy;
    private boolean active;
    private boolean hasBeacon;
    private List<String> connectedRoads;
    private int visitCount;
    private String iconType;
    private boolean isPublic;
    
    // Конструктор по умолчанию для JSON десериализации
    public Waypoint() {
        this.connectedRoads = new ArrayList<>();
    }
    
    public Waypoint(String name, BlockPos position, WaypointType type) {
        this(name, position, type, null);
    }
    
    public Waypoint(String name, BlockPos position, WaypointType type, String description) {
        this();
        this.name = name;
        this.position = position;
        this.type = type;
        this.description = description;
        this.creationTime = LocalDateTime.now();
        this.active = true;
        this.hasBeacon = false;
        this.visitCount = 0;
        this.iconType = type.getDefaultIcon();
        this.isPublic = true;
    }
    
    /**
     * Отмечает посещение путевой точки
     */
    public void markVisited(String visitorName) {
        this.lastVisited = LocalDateTime.now();
        this.visitCount++;
        
        if (this.createdBy == null) {
            this.createdBy = visitorName;
        }
    }
    
    /**
     * Добавляет связанную дорогу
     */
    public void addConnectedRoad(String roadId) {
        if (!connectedRoads.contains(roadId)) {
            connectedRoads.add(roadId);
        }
    }
    
    /**
     * Удаляет связанную дорогу
     */
    public void removeConnectedRoad(String roadId) {
        connectedRoads.remove(roadId);
    }
    
    /**
     * Вычисляет расстояние до указанной позиции
     */
    public double getDistanceFrom(BlockPos fromPos) {
        if (position == null || fromPos == null) return Double.MAX_VALUE;
        
        double dx = position.getX() - fromPos.getX();
        double dy = position.getY() - fromPos.getY();
        double dz = position.getZ() - fromPos.getZ();
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Проверяет, является ли путевая точка важной
     */
    public boolean isImportant() {
        return type == WaypointType.BASE || 
               type == WaypointType.TELEPORT_HUB || 
               type == WaypointType.RESOURCE_SITE ||
               visitCount > 10;
    }
    
    /**
     * Получает приоритет путевой точки для навигации
     */
    public int getNavigationPriority() {
        int priority = type.getPriority();
        
        // Бонус за частые посещения
        priority += Math.min(visitCount * 2, 50);
        
        // Бонус за наличие маяка
        if (hasBeacon) {
            priority += 20;
        }
        
        // Бонус за количество связанных дорог
        priority += connectedRoads.size() * 5;
        
        return priority;
    }
    
    /**
     * Получает краткое описание путевой точки
     */
    public String getShortDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(name).append(" (").append(type.getDisplayName()).append(")");
        
        if (hasBeacon) {
            desc.append(" [Маяк]");
        }
        
        if (visitCount > 0) {
            desc.append(" [Посещений: ").append(visitCount).append("]");
        }
        
        return desc.toString();
    }
    
    /**
     * Получает полное описание путевой точки
     */
    public String getFullDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("=== ").append(name).append(" ===\n");
        desc.append("Тип: ").append(type.getDisplayName()).append("\n");
        desc.append("Позиция: ").append(formatPosition()).append("\n");
        desc.append("Создана: ").append(creationTime).append("\n");
        
        if (createdBy != null) {
            desc.append("Создатель: ").append(createdBy).append("\n");
        }
        
        if (description != null && !description.isEmpty()) {
            desc.append("Описание: ").append(description).append("\n");
        }
        
        desc.append("Посещений: ").append(visitCount).append("\n");
        
        if (lastVisited != null) {
            desc.append("Последнее посещение: ").append(lastVisited).append("\n");
        }
        
        if (hasBeacon) {
            desc.append("Маяк: Да\n");
        }
        
        if (!connectedRoads.isEmpty()) {
            desc.append("Связанные дороги: ").append(connectedRoads.size()).append("\n");
        }
        
        desc.append("Статус: ").append(active ? "Активна" : "Неактивна");
        
        return desc.toString();
    }
    
    /**
     * Форматирует позицию для отображения
     */
    private String formatPosition() {
        if (position == null) return "неизвестно";
        return String.format("(%d, %d, %d)", position.getX(), position.getY(), position.getZ());
    }
    
    // Геттеры и сеттеры
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public void setPosition(BlockPos position) {
        this.position = position;
    }
    
    public WaypointType getType() {
        return type;
    }
    
    public void setType(WaypointType type) {
        this.type = type;
        if (this.iconType == null) {
            this.iconType = type.getDefaultIcon();
        }
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }
    
    public LocalDateTime getLastVisited() {
        return lastVisited;
    }
    
    public void setLastVisited(LocalDateTime lastVisited) {
        this.lastVisited = lastVisited;
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
    
    public boolean isHasBeacon() {
        return hasBeacon;
    }
    
    public void setHasBeacon(boolean hasBeacon) {
        this.hasBeacon = hasBeacon;
    }
    
    public List<String> getConnectedRoads() {
        return connectedRoads;
    }
    
    public void setConnectedRoads(List<String> connectedRoads) {
        this.connectedRoads = connectedRoads;
    }
    
    public int getVisitCount() {
        return visitCount;
    }
    
    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }
    
    public String getIconType() {
        return iconType;
    }
    
    public void setIconType(String iconType) {
        this.iconType = iconType;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    @Override
    public String toString() {
        return String.format("Waypoint{name='%s', type=%s, pos=%s, active=%s}", 
                           name, type, formatPosition(), active);
    }
}

