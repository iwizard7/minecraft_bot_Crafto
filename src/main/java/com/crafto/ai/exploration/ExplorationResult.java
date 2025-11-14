package com.crafto.ai.exploration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Результат операции исследования
 */
public class ExplorationResult {
    private boolean success;
    private String errorMessage;
    private List<ExploredArea> exploredAreas;
    private List<ResourceLocation> newResources;
    private List<DangerZone> newDangerZones;
    private LocalDateTime explorationTime;
    private long executionTimeMs;
    private String exploredBy;
    
    public ExplorationResult() {
        this.exploredAreas = new ArrayList<>();
        this.newResources = new ArrayList<>();
        this.newDangerZones = new ArrayList<>();
        this.explorationTime = LocalDateTime.now();
        this.success = false;
    }
    
    /**
     * Добавляет исследованную область к результату
     */
    public void addExploredArea(ExploredArea area) {
        exploredAreas.add(area);
        newResources.addAll(area.getResources());
    }
    
    /**
     * Добавляет опасную зону к результату
     */
    public void addDangerZone(DangerZone dangerZone) {
        newDangerZones.add(dangerZone);
    }
    
    /**
     * Получает общую статистику исследования
     */
    public ExplorationSummary getSummary() {
        int totalAreas = exploredAreas.size();
        int totalResources = newResources.size();
        int totalDangers = newDangerZones.size();
        
        int totalValue = newResources.stream().mapToInt(ResourceLocation::getValue).sum();
        
        long rareResources = newResources.stream().filter(ResourceLocation::isRare).count();
        
        return new ExplorationSummary(totalAreas, totalResources, totalDangers, 
                                    totalValue, (int) rareResources, executionTimeMs);
    }
    
    /**
     * Проверяет, было ли исследование успешным
     */
    public boolean wasSuccessful() {
        return success && !exploredAreas.isEmpty();
    }
    
    /**
     * Получает краткий отчет об исследовании
     */
    public String getReport() {
        if (!success) {
            return "Исследование неудачно: " + (errorMessage != null ? errorMessage : "Неизвестная ошибка");
        }
        
        ExplorationSummary summary = getSummary();
        StringBuilder report = new StringBuilder();
        
        report.append("=== ОТЧЕТ ОБ ИССЛЕДОВАНИИ ===\n");
        report.append("Время: ").append(explorationTime).append("\n");
        report.append("Исследователь: ").append(exploredBy != null ? exploredBy : "Неизвестно").append("\n");
        report.append("Длительность: ").append(executionTimeMs).append(" мс\n\n");
        
        report.append("РЕЗУЛЬТАТЫ:\n");
        report.append("- Исследовано областей: ").append(summary.getTotalAreas()).append("\n");
        report.append("- Найдено ресурсов: ").append(summary.getTotalResources()).append("\n");
        report.append("- Редких ресурсов: ").append(summary.getRareResources()).append("\n");
        report.append("- Общая ценность: ").append(summary.getTotalValue()).append("\n");
        report.append("- Опасных зон: ").append(summary.getTotalDangers()).append("\n\n");
        
        if (!newResources.isEmpty()) {
            report.append("НАЙДЕННЫЕ РЕСУРСЫ:\n");
            newResources.stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .forEach(resource -> {
                    report.append("- ").append(resource.getResourceType())
                          .append(" (ценность: ").append(resource.getValue())
                          .append(", позиция: ").append(resource.getPosition())
                          .append(")\n");
                });
        }
        
        if (!newDangerZones.isEmpty()) {
            report.append("\nОПАСНЫЕ ЗОНЫ:\n");
            newDangerZones.forEach(danger -> {
                report.append("- ").append(danger.getCoordinate())
                      .append(": ").append(danger.getDangerLevel().getLevelDescription())
                      .append(" (").append(danger.getDangerLevel().getPrimaryThreat())
                      .append(")\n");
            });
        }
        
        return report.toString();
    }
    
    // Геттеры и сеттеры
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public List<ExploredArea> getExploredAreas() {
        return exploredAreas;
    }
    
    public void setExploredAreas(List<ExploredArea> exploredAreas) {
        this.exploredAreas = exploredAreas;
    }
    
    public List<ResourceLocation> getNewResources() {
        return newResources;
    }
    
    public void setNewResources(List<ResourceLocation> newResources) {
        this.newResources = newResources;
    }
    
    public List<DangerZone> getNewDangerZones() {
        return newDangerZones;
    }
    
    public void setNewDangerZones(List<DangerZone> newDangerZones) {
        this.newDangerZones = newDangerZones;
    }
    
    public LocalDateTime getExplorationTime() {
        return explorationTime;
    }
    
    public void setExplorationTime(LocalDateTime explorationTime) {
        this.explorationTime = explorationTime;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public String getExploredBy() {
        return exploredBy;
    }
    
    public void setExploredBy(String exploredBy) {
        this.exploredBy = exploredBy;
    }
}

/**
 * Краткая сводка результатов исследования
 */
class ExplorationSummary {
    private int totalAreas;
    private int totalResources;
    private int totalDangers;
    private int totalValue;
    private int rareResources;
    private long executionTimeMs;
    
    public ExplorationSummary(int totalAreas, int totalResources, int totalDangers, 
                            int totalValue, int rareResources, long executionTimeMs) {
        this.totalAreas = totalAreas;
        this.totalResources = totalResources;
        this.totalDangers = totalDangers;
        this.totalValue = totalValue;
        this.rareResources = rareResources;
        this.executionTimeMs = executionTimeMs;
    }
    
    // Геттеры
    
    public int getTotalAreas() {
        return totalAreas;
    }
    
    public int getTotalResources() {
        return totalResources;
    }
    
    public int getTotalDangers() {
        return totalDangers;
    }
    
    public int getTotalValue() {
        return totalValue;
    }
    
    public int getRareResources() {
        return rareResources;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
}