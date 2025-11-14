package com.crafto.ai.exploration;

import java.util.Map;

/**
 * Статистика исследования
 */
public class ExplorationStats {
    private int totalExploredAreas;
    private int totalResources;
    private int dangerousAreas;
    private Map<String, Integer> resourceCounts;
    private double averageAreaValue;
    private String mostValuableResource;
    private String mostCommonBiome;
    
    public ExplorationStats(int totalExploredAreas, int totalResources, int dangerousAreas, 
                          Map<String, Integer> resourceCounts) {
        this.totalExploredAreas = totalExploredAreas;
        this.totalResources = totalResources;
        this.dangerousAreas = dangerousAreas;
        this.resourceCounts = resourceCounts;
        calculateDerivedStats();
    }
    
    /**
     * Вычисляет производные статистики
     */
    private void calculateDerivedStats() {
        // Средняя ценность области
        if (totalExploredAreas > 0) {
            averageAreaValue = (double) totalResources / totalExploredAreas;
        }
        
        // Самый ценный ресурс
        mostValuableResource = resourceCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Нет данных");
    }
    
    /**
     * Получает процент опасных областей
     */
    public double getDangerPercentage() {
        if (totalExploredAreas == 0) return 0.0;
        return (double) dangerousAreas / totalExploredAreas * 100.0;
    }
    
    /**
     * Получает эффективность исследования (ресурсов на область)
     */
    public double getExplorationEfficiency() {
        return averageAreaValue;
    }
    
    /**
     * Получает текстовый отчет о статистике
     */
    public String getReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== СТАТИСТИКА ИССЛЕДОВАНИЯ ===\n");
        report.append("Исследовано областей: ").append(totalExploredAreas).append("\n");
        report.append("Найдено ресурсов: ").append(totalResources).append("\n");
        report.append("Опасных зон: ").append(dangerousAreas)
              .append(" (").append(String.format("%.1f", getDangerPercentage())).append("%)\n");
        report.append("Эффективность: ").append(String.format("%.2f", getExplorationEfficiency()))
              .append(" ресурсов/область\n");
        report.append("Самый частый ресурс: ").append(mostValuableResource).append("\n\n");
        
        if (!resourceCounts.isEmpty()) {
            report.append("РАСПРЕДЕЛЕНИЕ РЕСУРСОВ:\n");
            resourceCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> {
                        report.append("- ").append(entry.getKey())
                              .append(": ").append(entry.getValue()).append("\n");
                    });
        }
        
        return report.toString();
    }
    
    // Геттеры и сеттеры
    
    public int getTotalExploredAreas() {
        return totalExploredAreas;
    }
    
    public void setTotalExploredAreas(int totalExploredAreas) {
        this.totalExploredAreas = totalExploredAreas;
    }
    
    public int getTotalResources() {
        return totalResources;
    }
    
    public void setTotalResources(int totalResources) {
        this.totalResources = totalResources;
    }
    
    public int getDangerousAreas() {
        return dangerousAreas;
    }
    
    public void setDangerousAreas(int dangerousAreas) {
        this.dangerousAreas = dangerousAreas;
    }
    
    public Map<String, Integer> getResourceCounts() {
        return resourceCounts;
    }
    
    public void setResourceCounts(Map<String, Integer> resourceCounts) {
        this.resourceCounts = resourceCounts;
    }
    
    public double getAverageAreaValue() {
        return averageAreaValue;
    }
    
    public void setAverageAreaValue(double averageAreaValue) {
        this.averageAreaValue = averageAreaValue;
    }
    
    public String getMostValuableResource() {
        return mostValuableResource;
    }
    
    public void setMostValuableResource(String mostValuableResource) {
        this.mostValuableResource = mostValuableResource;
    }
    
    public String getMostCommonBiome() {
        return mostCommonBiome;
    }
    
    public void setMostCommonBiome(String mostCommonBiome) {
        this.mostCommonBiome = mostCommonBiome;
    }
}