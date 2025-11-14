package com.crafto.ai.exploration;

/**
 * Типы дорог
 */
public enum RoadType {
    DIRT_PATH("Грунтовая тропа", 1.0, 30),
    STONE_ROAD("Каменная дорога", 1.2, 60),
    NETHER_HIGHWAY("Незер магистраль", 8.0, 120),
    WATER_CANAL("Водный канал", 0.8, 25),
    RAIL_TRACK("Железная дорога", 2.0, 100),
    ICE_ROAD("Ледяная дорога", 1.5, 80);
    
    private final String displayName;
    private final double speedMultiplier;
    private final int buildCost;
    
    RoadType(String displayName, double speedMultiplier, int buildCost) {
        this.displayName = displayName;
        this.speedMultiplier = speedMultiplier;
        this.buildCost = buildCost;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public double getSpeedMultiplier() {
        return speedMultiplier;
    }
    
    public int getBuildCost() {
        return buildCost;
    }
    
    public int getPriority() {
        return buildCost; // Используем buildCost как приоритет
    }
}