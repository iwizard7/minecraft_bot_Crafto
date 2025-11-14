package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import java.time.LocalDateTime;

/**
 * Маркер на карте для отметки важных мест
 */
public class MapMarker {
    private String markerId;
    private BlockPos position;
    private MarkerType type;
    private String name;
    private String description;
    private LocalDateTime creationTime;
    private String createdBy;
    private boolean visible;
    private String iconColor;
    private String iconType;
    private int priority;
    private boolean shared;
    private String category;
    
    // Конструктор по умолчанию для JSON десериализации
    public MapMarker() {
    }
    
    public MapMarker(String markerId, BlockPos position, MarkerType type, String name, String description) {
        this.markerId = markerId;
        this.position = position;
        this.type = type;
        this.name = name;
        this.description = description;
        this.creationTime = LocalDateTime.now();
        this.visible = true;
        this.iconColor = type.getDefaultColor();
        this.iconType = type.getDefaultIcon();
        this.priority = type.getDefaultPriority();
        this.shared = false;
        this.category = type.getDisplayName();
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
     * Проверяет, является ли маркер важным
     */
    public boolean isImportant() {
        return priority > 50 || 
               type == MarkerType.BASE || 
               type == MarkerType.DANGER || 
               type == MarkerType.TELEPORT_HUB;
    }
    
    /**
     * Получает краткое описание маркера
     */
    public String getShortDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(name);
        if (type != null) {
            desc.append(" (").append(type.getDisplayName()).append(")");
        }
        return desc.toString();
    }
    
    /**
     * Получает полное описание маркера
     */
    public String getFullDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("=== ").append(name).append(" ===\n");
        desc.append("Тип: ").append(type != null ? type.getDisplayName() : "Неизвестно").append("\n");
        desc.append("Позиция: ").append(formatPosition()).append("\n");
        desc.append("Создан: ").append(creationTime).append("\n");
        
        if (createdBy != null) {
            desc.append("Создатель: ").append(createdBy).append("\n");
        }
        
        if (description != null && !description.isEmpty()) {
            desc.append("Описание: ").append(description).append("\n");
        }
        
        if (category != null && !category.equals(type.getDisplayName())) {
            desc.append("Категория: ").append(category).append("\n");
        }
        
        desc.append("Приоритет: ").append(priority).append("\n");
        desc.append("Видимый: ").append(visible ? "Да" : "Нет").append("\n");
        desc.append("Общий доступ: ").append(shared ? "Да" : "Нет");
        
        return desc.toString();
    }
    
    /**
     * Форматирует позицию для отображения
     */
    private String formatPosition() {
        if (position == null) return "неизвестно";
        return String.format("(%d, %d, %d)", position.getX(), position.getY(), position.getZ());
    }
    
    /**
     * Клонирует маркер с новым ID
     */
    public MapMarker clone(String newMarkerId) {
        MapMarker clone = new MapMarker(newMarkerId, position, type, name, description);
        clone.createdBy = this.createdBy;
        clone.visible = this.visible;
        clone.iconColor = this.iconColor;
        clone.iconType = this.iconType;
        clone.priority = this.priority;
        clone.shared = this.shared;
        clone.category = this.category;
        return clone;
    }
    
    // Геттеры и сеттеры
    
    public String getMarkerId() {
        return markerId;
    }
    
    public void setMarkerId(String markerId) {
        this.markerId = markerId;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public void setPosition(BlockPos position) {
        this.position = position;
    }
    
    public MarkerType getType() {
        return type;
    }
    
    public void setType(MarkerType type) {
        this.type = type;
        if (this.iconColor == null) {
            this.iconColor = type.getDefaultColor();
        }
        if (this.iconType == null) {
            this.iconType = type.getDefaultIcon();
        }
        if (this.priority == 0) {
            this.priority = type.getDefaultPriority();
        }
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public String getIconColor() {
        return iconColor;
    }
    
    public void setIconColor(String iconColor) {
        this.iconColor = iconColor;
    }
    
    public String getIconType() {
        return iconType;
    }
    
    public void setIconType(String iconType) {
        this.iconType = iconType;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public boolean isShared() {
        return shared;
    }
    
    public void setShared(boolean shared) {
        this.shared = shared;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    @Override
    public String toString() {
        return String.format("MapMarker{id='%s', name='%s', type=%s, pos=%s, visible=%s}", 
                           markerId, name, type, formatPosition(), visible);
    }
}

/**
 * Типы маркеров на карте
 */
enum MarkerType {
    RESOURCE("Ресурс", "resource", "YELLOW", 60),
    STRUCTURE("Структура", "structure", "ORANGE", 70),
    BASE("База", "base", "GREEN", 90),
    WAYPOINT("Путевая точка", "waypoint", "BLUE", 50),
    DANGER("Опасность", "danger", "RED", 80),
    TELEPORT_HUB("Телепорт", "teleport", "PURPLE", 85),
    LANDMARK("Ориентир", "landmark", "CYAN", 40),
    FARM("Ферма", "farm", "LIME", 55),
    MINE("Шахта", "mine", "GRAY", 65),
    PORTAL("Портал", "portal", "MAGENTA", 75),
    CUSTOM("Пользовательский", "custom", "WHITE", 30);
    
    private final String displayName;
    private final String defaultIcon;
    private final String defaultColor;
    private final int defaultPriority;
    
    MarkerType(String displayName, String defaultIcon, String defaultColor, int defaultPriority) {
        this.displayName = displayName;
        this.defaultIcon = defaultIcon;
        this.defaultColor = defaultColor;
        this.defaultPriority = defaultPriority;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDefaultIcon() {
        return defaultIcon;
    }
    
    public String getDefaultColor() {
        return defaultColor;
    }
    
    public int getDefaultPriority() {
        return defaultPriority;
    }
}