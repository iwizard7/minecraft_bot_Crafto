package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Навигационный путь между точками
 */
public class NavigationPath {
    private List<BlockPos> waypoints;
    private double totalDistance;
    private int estimatedTravelTime;
    private LocalDateTime creationTime;
    private String pathType;
    private boolean optimized;
    private List<String> instructions;
    
    public NavigationPath(List<BlockPos> waypoints) {
        this.waypoints = new ArrayList<>(waypoints);
        this.creationTime = LocalDateTime.now();
        this.pathType = "STANDARD";
        this.optimized = false;
        this.instructions = new ArrayList<>();
        calculateMetrics();
        generateInstructions();
    }
    
    /**
     * Вычисляет метрики пути
     */
    private void calculateMetrics() {
        if (waypoints.size() < 2) {
            totalDistance = 0.0;
            estimatedTravelTime = 0;
            return;
        }
        
        totalDistance = 0.0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            BlockPos from = waypoints.get(i);
            BlockPos to = waypoints.get(i + 1);
            totalDistance += calculateDistance(from, to);
        }
        
        // Оценка времени путешествия (базовая скорость 4.3 блока/сек)
        estimatedTravelTime = (int) (totalDistance / 4.3);
    }
    
    /**
     * Вычисляет расстояние между двумя точками
     */
    private double calculateDistance(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Генерирует пошаговые инструкции для навигации
     */
    private void generateInstructions() {
        instructions.clear();
        
        if (waypoints.size() < 2) {
            instructions.add("Путь не найден");
            return;
        }
        
        instructions.add("Начать путешествие от " + formatPosition(waypoints.get(0)));
        
        for (int i = 1; i < waypoints.size(); i++) {
            BlockPos current = waypoints.get(i - 1);
            BlockPos next = waypoints.get(i);
            
            String direction = getDirection(current, next);
            double distance = calculateDistance(current, next);
            
            if (i == waypoints.size() - 1) {
                instructions.add(String.format("Идти %s %.1f блоков до пункта назначения %s", 
                    direction, distance, formatPosition(next)));
            } else {
                instructions.add(String.format("Идти %s %.1f блоков до точки %s", 
                    direction, distance, formatPosition(next)));
            }
        }
        
        instructions.add("Прибытие в пункт назначения");
    }
    
    /**
     * Определяет направление между двумя точками
     */
    private String getDirection(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        int dy = to.getY() - from.getY();
        
        String horizontal = "";
        String vertical = "";
        
        // Горизонтальное направление
        if (Math.abs(dx) > Math.abs(dz)) {
            horizontal = dx > 0 ? "на восток" : "на запад";
        } else if (Math.abs(dz) > Math.abs(dx)) {
            horizontal = dz > 0 ? "на юг" : "на север";
        } else if (dx != 0 && dz != 0) {
            String ns = dz > 0 ? "юго" : "северо";
            String ew = dx > 0 ? "восток" : "запад";
            horizontal = "на " + ns + "-" + ew;
        }
        
        // Вертикальное направление
        if (Math.abs(dy) > 5) { // Значительное изменение высоты
            if (dy > 0) {
                vertical = " (вверх)";
            } else {
                vertical = " (вниз)";
            }
        }
        
        return horizontal + vertical;
    }
    
    /**
     * Оптимизирует путь, удаляя ненужные промежуточные точки
     */
    public void optimize() {
        if (waypoints.size() <= 2) {
            optimized = true;
            return;
        }
        
        List<BlockPos> optimizedWaypoints = new ArrayList<>();
        optimizedWaypoints.add(waypoints.get(0)); // Начальная точка
        
        for (int i = 1; i < waypoints.size() - 1; i++) {
            BlockPos prev = optimizedWaypoints.get(optimizedWaypoints.size() - 1);
            BlockPos current = waypoints.get(i);
            BlockPos next = waypoints.get(i + 1);
            
            // Проверяем, можно ли пропустить текущую точку
            if (!canSkipWaypoint(prev, current, next)) {
                optimizedWaypoints.add(current);
            }
        }
        
        optimizedWaypoints.add(waypoints.get(waypoints.size() - 1)); // Конечная точка
        
        this.waypoints = optimizedWaypoints;
        this.optimized = true;
        calculateMetrics();
        generateInstructions();
    }
    
    /**
     * Проверяет, можно ли пропустить промежуточную точку
     */
    private boolean canSkipWaypoint(BlockPos prev, BlockPos current, BlockPos next) {
        // Простая проверка: если точки почти на одной линии, можно пропустить
        double directDistance = calculateDistance(prev, next);
        double viaCurrentDistance = calculateDistance(prev, current) + calculateDistance(current, next);
        
        // Если обход через текущую точку добавляет менее 10% к расстоянию, можно пропустить
        return (viaCurrentDistance - directDistance) / directDistance < 0.1;
    }
    
    /**
     * Получает следующую точку пути от текущей позиции
     */
    public BlockPos getNextWaypoint(BlockPos currentPos) {
        if (waypoints.isEmpty()) return null;
        
        // Находим ближайшую точку в пути
        int closestIndex = 0;
        double closestDistance = Double.MAX_VALUE;
        
        for (int i = 0; i < waypoints.size(); i++) {
            double distance = calculateDistance(currentPos, waypoints.get(i));
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        
        // Возвращаем следующую точку после ближайшей
        if (closestIndex < waypoints.size() - 1) {
            return waypoints.get(closestIndex + 1);
        }
        
        return null; // Достигли конца пути
    }
    
    /**
     * Проверяет, достигнут ли конец пути
     */
    public boolean isCompleted(BlockPos currentPos) {
        if (waypoints.isEmpty()) return true;
        
        BlockPos destination = waypoints.get(waypoints.size() - 1);
        return calculateDistance(currentPos, destination) < 5.0; // В пределах 5 блоков
    }
    
    /**
     * Получает прогресс прохождения пути (0.0 - 1.0)
     */
    public double getProgress(BlockPos currentPos) {
        if (waypoints.size() < 2) return 1.0;
        
        BlockPos start = waypoints.get(0);
        BlockPos end = waypoints.get(waypoints.size() - 1);
        
        double totalPathDistance = calculateDistance(start, end);
        double remainingDistance = calculateDistance(currentPos, end);
        
        return Math.max(0.0, Math.min(1.0, 1.0 - (remainingDistance / totalPathDistance)));
    }
    
    /**
     * Форматирует позицию для отображения
     */
    private String formatPosition(BlockPos pos) {
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
    
    /**
     * Получает краткое описание пути
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Путь из ").append(waypoints.size()).append(" точек\n");
        summary.append("Общее расстояние: ").append(String.format("%.1f", totalDistance)).append(" блоков\n");
        summary.append("Время в пути: ").append(formatTime(estimatedTravelTime)).append("\n");
        summary.append("Тип пути: ").append(pathType).append("\n");
        summary.append("Оптимизирован: ").append(optimized ? "Да" : "Нет");
        
        return summary.toString();
    }
    
    // Геттеры и сеттеры
    
    public List<BlockPos> getWaypoints() {
        return waypoints;
    }
    
    public void setWaypoints(List<BlockPos> waypoints) {
        this.waypoints = waypoints;
        calculateMetrics();
        generateInstructions();
    }
    
    public double getTotalDistance() {
        return totalDistance;
    }
    
    public int getEstimatedTravelTime() {
        return estimatedTravelTime;
    }
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }
    
    public String getPathType() {
        return pathType;
    }
    
    public void setPathType(String pathType) {
        this.pathType = pathType;
    }
    
    public boolean isOptimized() {
        return optimized;
    }
    
    public List<String> getInstructions() {
        return instructions;
    }
    
    @Override
    public String toString() {
        return String.format("NavigationPath{waypoints=%d, distance=%.1f, time=%s, optimized=%s}", 
                           waypoints.size(), totalDistance, formatTime(estimatedTravelTime), optimized);
    }
}