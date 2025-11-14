package com.crafto.ai.exploration;

import java.util.Map;

/**
 * Статистика навигационной системы
 */
public class NavigationStats {
    private int totalWaypoints;
    private int activeWaypoints;
    private int totalRoads;
    private int activeRoads;
    private int totalTeleportHubs;
    private Map<String, Integer> waypointTypeCount;
    private double totalRoadDistance;
    private double averageRoadLength;
    private double networkConnectivity;
    
    public NavigationStats(int totalWaypoints, int activeWaypoints, int totalRoads, 
                         int activeRoads, int totalTeleportHubs, Map<String, Integer> waypointTypeCount, 
                         double totalRoadDistance) {
        this.totalWaypoints = totalWaypoints;
        this.activeWaypoints = activeWaypoints;
        this.totalRoads = totalRoads;
        this.activeRoads = activeRoads;
        this.totalTeleportHubs = totalTeleportHubs;
        this.waypointTypeCount = waypointTypeCount;
        this.totalRoadDistance = totalRoadDistance;
        calculateDerivedStats();
    }
    
    /**
     * Вычисляет производные статистики
     */
    private void calculateDerivedStats() {
        // Средняя длина дороги
        if (activeRoads > 0) {
            averageRoadLength = totalRoadDistance / activeRoads;
        }
        
        // Связность сети (упрощенная метрика)
        if (activeWaypoints > 0) {
            networkConnectivity = (double) activeRoads / activeWaypoints;
        }
    }
    
    /**
     * Получает процент активных путевых точек
     */
    public double getActiveWaypointPercentage() {
        if (totalWaypoints == 0) return 0.0;
        return (double) activeWaypoints / totalWaypoints * 100.0;
    }
    
    /**
     * Получает процент активных дорог
     */
    public double getActiveRoadPercentage() {
        if (totalRoads == 0) return 0.0;
        return (double) activeRoads / totalRoads * 100.0;
    }
    
    /**
     * Получает плотность сети (дорог на путевую точку)
     */
    public double getNetworkDensity() {
        return networkConnectivity;
    }
    
    /**
     * Получает оценку качества навигационной сети
     */
    public String getNetworkQuality() {
        double connectivity = getNetworkDensity();
        double activePercentage = getActiveWaypointPercentage();
        
        if (connectivity > 2.0 && activePercentage > 90) {
            return "Отличная";
        } else if (connectivity > 1.5 && activePercentage > 80) {
            return "Хорошая";
        } else if (connectivity > 1.0 && activePercentage > 60) {
            return "Удовлетворительная";
        } else {
            return "Требует улучшения";
        }
    }
    
    /**
     * Получает текстовый отчет о статистике
     */
    public String getReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== СТАТИСТИКА НАВИГАЦИОННОЙ СИСТЕМЫ ===\n");
        report.append("Путевые точки: ").append(activeWaypoints).append("/").append(totalWaypoints)
              .append(" (").append(String.format("%.1f", getActiveWaypointPercentage())).append("%)\n");
        report.append("Дороги: ").append(activeRoads).append("/").append(totalRoads)
              .append(" (").append(String.format("%.1f", getActiveRoadPercentage())).append("%)\n");
        report.append("Телепорт хабы: ").append(totalTeleportHubs).append("\n");
        report.append("Общая длина дорог: ").append(String.format("%.1f", totalRoadDistance)).append(" блоков\n");
        report.append("Средняя длина дороги: ").append(String.format("%.1f", averageRoadLength)).append(" блоков\n");
        report.append("Связность сети: ").append(String.format("%.2f", networkConnectivity)).append("\n");
        report.append("Качество сети: ").append(getNetworkQuality()).append("\n\n");
        
        if (!waypointTypeCount.isEmpty()) {
            report.append("РАСПРЕДЕЛЕНИЕ ПУТЕВЫХ ТОЧЕК ПО ТИПАМ:\n");
            waypointTypeCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        report.append("- ").append(entry.getKey())
                              .append(": ").append(entry.getValue()).append("\n");
                    });
        }
        
        return report.toString();
    }
    
    // Геттеры и сеттеры
    
    public int getTotalWaypoints() {
        return totalWaypoints;
    }
    
    public void setTotalWaypoints(int totalWaypoints) {
        this.totalWaypoints = totalWaypoints;
    }
    
    public int getActiveWaypoints() {
        return activeWaypoints;
    }
    
    public void setActiveWaypoints(int activeWaypoints) {
        this.activeWaypoints = activeWaypoints;
    }
    
    public int getTotalRoads() {
        return totalRoads;
    }
    
    public void setTotalRoads(int totalRoads) {
        this.totalRoads = totalRoads;
    }
    
    public int getActiveRoads() {
        return activeRoads;
    }
    
    public void setActiveRoads(int activeRoads) {
        this.activeRoads = activeRoads;
    }
    
    public int getTotalTeleportHubs() {
        return totalTeleportHubs;
    }
    
    public void setTotalTeleportHubs(int totalTeleportHubs) {
        this.totalTeleportHubs = totalTeleportHubs;
    }
    
    public Map<String, Integer> getWaypointTypeCount() {
        return waypointTypeCount;
    }
    
    public void setWaypointTypeCount(Map<String, Integer> waypointTypeCount) {
        this.waypointTypeCount = waypointTypeCount;
    }
    
    public double getTotalRoadDistance() {
        return totalRoadDistance;
    }
    
    public void setTotalRoadDistance(double totalRoadDistance) {
        this.totalRoadDistance = totalRoadDistance;
    }
    
    public double getAverageRoadLength() {
        return averageRoadLength;
    }
    
    public double getNetworkConnectivity() {
        return networkConnectivity;
    }
}