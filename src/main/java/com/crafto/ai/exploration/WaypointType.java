package com.crafto.ai.exploration;

/**
 * Типы путевых точек
 */
public enum WaypointType {
    BASE("Базы", "base", 100),
    RESOURCE_SITE("Месторождение", "resource", 80),
    TELEPORT_HUB("Телепорт хаб", "teleport", 90),
    LANDMARK("Ориентир", "landmark", 60),
    DANGER_ZONE("Опасная зона", "danger", 40),
    TRADING_POST("Торговый пост", "trade", 70),
    FARM("Ферма", "farm", 50),
    MINE("Шахта", "mine", 75),
    PORTAL("Портал", "portal", 95);
    
    private final String displayName;
    private final String shortName;
    private final int priority;
    
    WaypointType(String displayName, String shortName, int priority) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.priority = priority;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getShortName() {
        return shortName;
    }
    
    public String getDefaultIcon() {
        return shortName;
    }
    
    public int getPriority() {
        return priority;
    }
}