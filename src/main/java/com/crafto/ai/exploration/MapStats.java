package com.crafto.ai.exploration;

import java.util.Map;

/**
 * Статистика системы карт
 */
public class MapStats {
    private int totalChunks;
    private int totalMarkers;
    private int sharedMapCount;
    private Map<String, Integer> markerTypeCount;
    private long exploredArea;
    private double averageChunkValue;
    private String mostCommonMarkerType;
    private String mostCommonBiome;
    
    public MapStats(int totalChunks, int totalMarkers, int sharedMapCount, 
                   Map<String, Integer> markerTypeCount, long exploredArea) {
        this.totalChunks = totalChunks;
        this.totalMarkers = totalMarkers;
        this.sharedMapCount = sharedMapCount;
        this.markerTypeCount = markerTypeCount;
        this.exploredArea = exploredArea;
        calculateDerivedStats();
    }
    
    /**
     * Вычисляет производные статистики
     */
    private void calculateDerivedStats() {
        // Средняя ценность чанка (упрощенная метрика)
        if (totalChunks > 0) {
            averageChunkValue = (double) totalMarkers / totalChunks;
        }
        
        // Самый частый тип маркера
        mostCommonMarkerType = markerTypeCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Нет данных");
    }
    
    /**
     * Получает плотность маркеров (маркеров на чанк)
     */
    public double getMarkerDensity() {
        return averageChunkValue;
    }
    
    /**
     * Получает покрытие карты в квадратных километрах
     */
    public double getCoverageInKm2() {
        // 1 блок = 1 метр, 1 км² = 1,000,000 м²
        return exploredArea / 1_000_000.0;
    }
    
    /**
     * Получает эффективность картографирования
     */
    public String getMappingEfficiency() {
        double density = getMarkerDensity();
        
        if (density > 5.0) {
            return "Очень высокая";
        } else if (density > 2.0) {
            return "Высокая";
        } else if (density > 1.0) {
            return "Средняя";
        } else if (density > 0.5) {
            return "Низкая";
        } else {
            return "Очень низкая";
        }
    }
    
    /**
     * Получает текстовый отчет о статистике
     */
    public String getReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== СТАТИСТИКА СИСТЕМЫ КАРТ ===\n");
        report.append("Исследованных чанков: ").append(totalChunks).append("\n");
        report.append("Общих маркеров: ").append(totalMarkers).append("\n");
        report.append("Совместных карт: ").append(sharedMapCount).append("\n");
        report.append("Исследованная область: ").append(exploredArea).append(" блоков²")
              .append(" (").append(String.format("%.2f", getCoverageInKm2())).append(" км²)\n");
        report.append("Плотность маркеров: ").append(String.format("%.2f", getMarkerDensity()))
              .append(" маркеров/чанк\n");
        report.append("Эффективность картографирования: ").append(getMappingEfficiency()).append("\n");
        report.append("Самый частый тип маркера: ").append(mostCommonMarkerType).append("\n\n");
        
        if (!markerTypeCount.isEmpty()) {
            report.append("РАСПРЕДЕЛЕНИЕ МАРКЕРОВ ПО ТИПАМ:\n");
            markerTypeCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        double percentage = (double) entry.getValue() / totalMarkers * 100.0;
                        report.append("- ").append(entry.getKey())
                              .append(": ").append(entry.getValue())
                              .append(" (").append(String.format("%.1f", percentage)).append("%)\n");
                    });
        }
        
        return report.toString();
    }
    
    // Геттеры и сеттеры
    
    public int getTotalChunks() {
        return totalChunks;
    }
    
    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }
    
    public int getTotalMarkers() {
        return totalMarkers;
    }
    
    public void setTotalMarkers(int totalMarkers) {
        this.totalMarkers = totalMarkers;
    }
    
    public int getSharedMapCount() {
        return sharedMapCount;
    }
    
    public void setSharedMapCount(int sharedMapCount) {
        this.sharedMapCount = sharedMapCount;
    }
    
    public Map<String, Integer> getMarkerTypeCount() {
        return markerTypeCount;
    }
    
    public void setMarkerTypeCount(Map<String, Integer> markerTypeCount) {
        this.markerTypeCount = markerTypeCount;
    }
    
    public long getExploredArea() {
        return exploredArea;
    }
    
    public void setExploredArea(long exploredArea) {
        this.exploredArea = exploredArea;
    }
    
    public double getAverageChunkValue() {
        return averageChunkValue;
    }
    
    public void setAverageChunkValue(double averageChunkValue) {
        this.averageChunkValue = averageChunkValue;
    }
    
    public String getMostCommonMarkerType() {
        return mostCommonMarkerType;
    }
    
    public void setMostCommonMarkerType(String mostCommonMarkerType) {
        this.mostCommonMarkerType = mostCommonMarkerType;
    }
    
    public String getMostCommonBiome() {
        return mostCommonBiome;
    }
    
    public void setMostCommonBiome(String mostCommonBiome) {
        this.mostCommonBiome = mostCommonBiome;
    }
}