package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Класс интеграции системы исследования с основным Crafto AI
 * Предоставляет упрощенный API для использования всех компонентов системы
 */
public class ExplorationIntegration {
    
    private final ExplorationSystem exploration;
    private final WaypointSystem waypoints;
    private final MapSystem maps;
    private final String craftoName;
    
    public ExplorationIntegration(Level world, String craftoName) {
        this.craftoName = craftoName;
        this.exploration = new ExplorationSystem(world, craftoName);
        this.waypoints = new WaypointSystem(world, craftoName);
        this.maps = new MapSystem(world, craftoName, exploration, waypoints);
    }
    
    /**
     * Быстрый старт: исследовать область и создать базовую инфраструктуру
     */
    public CompletableFuture<Void> quickStart(BlockPos startPos, int radius) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 1. Создать базовую точку
                waypoints.createWaypoint("Home", startPos, WaypointType.BASE, "Домашняя база");
                
                // 2. Исследовать область
                ExplorationResult result = exploration.exploreArea(startPos, radius).join();
                
                // 3. Создать путевые точки для найденных ресурсов
                createResourceWaypoints(result);
                
                // 4. Создать карту области
                maps.createSharedMap("HomeArea", startPos, radius * 2);
                
                System.out.println("Быстрый старт завершен!");
                System.out.println("Исследовано: " + result.getExploredAreas().size() + " областей");
                System.out.println("Найдено ресурсов: " + result.getNewResources().size());
                
            } catch (Exception e) {
                System.err.println("Ошибка при быстром старте: " + e.getMessage());
            }
        });
    }
    
    /**
     * Автоматическое исследование большой области по частям
     */
    public CompletableFuture<Void> autoExplore(BlockPos center, int totalRadius) {
        return CompletableFuture.runAsync(() -> {
            try {
                exploration.scheduleExploration(center, totalRadius);
                
                int completed = 0;
                while (true) {
                    Optional<ExplorationTask> task = exploration.getNextExplorationTask();
                    if (!task.isPresent()) {
                        break;
                    }
                    
                    ChunkCoordinate coord = task.get().getCoordinate();
                    BlockPos chunkCenter = new BlockPos(coord.x << 4, 64, coord.z << 4);
                    
                    ExplorationResult result = exploration.exploreArea(chunkCenter, 64).join();
                    createResourceWaypoints(result);
                    
                    completed++;
                    if (completed % 10 == 0) {
                        System.out.println("Исследовано чанков: " + completed);
                        // Сохранить прогресс
                        saveAllData();
                    }
                    
                    // Небольшая пауза для предотвращения перегрузки
                    Thread.sleep(500);
                }
                
                System.out.println("Автоматическое исследование завершено! Всего чанков: " + completed);
                
            } catch (Exception e) {
                System.err.println("Ошибка при автоматическом исследовании: " + e.getMessage());
            }
        });
    }
    
    /**
     * Создать сеть быстрого перемещения
     */
    public void setupFastTravel(BlockPos hubPos) {
        try {
            // Создать центральный хаб
            TeleportHub centralHub = waypoints.createTeleportHub("CentralHub", hubPos, "Центральный телепортационный хаб");
            
            // Добавить все важные точки к хабу
            List<Waypoint> importantWaypoints = waypoints.getWaypointsByType(WaypointType.BASE);
            importantWaypoints.addAll(waypoints.getWaypointsByType(WaypointType.RESOURCE_SITE));
            importantWaypoints.addAll(waypoints.getWaypointsByType(WaypointType.TRADING_POST));
            
            for (Waypoint wp : importantWaypoints) {
                centralHub.addDestination(wp.getName());
            }
            
            System.out.println("Система быстрого перемещения настроена!");
            System.out.println("Центральный хаб: " + hubPos);
            System.out.println("Доступных точек: " + centralHub.getDestinations().size());
            
        } catch (Exception e) {
            System.err.println("Ошибка при настройке быстрого перемещения: " + e.getMessage());
        }
    }
    
    /**
     * Найти путь к ближайшему ресурсу определенного типа
     */
    public Optional<NavigationPath> findNearestResource(BlockPos from, String resourceType) {
        List<ResourceLocation> resources = exploration.getResourceLocations(resourceType);
        
        if (resources.isEmpty()) {
            return Optional.empty();
        }
        
        // Найти ближайший ресурс
        ResourceLocation nearest = resources.stream()
            .min((r1, r2) -> Double.compare(
                calculateDistance(from, r1.getPosition()),
                calculateDistance(from, r2.getPosition())
            ))
            .orElse(null);
        
        if (nearest != null) {
            return waypoints.findPath(from, nearest.getPosition());
        }
        
        return Optional.empty();
    }
    
    /**
     * Получить отчет о текущем состоянии исследования
     */
    public String getExplorationReport() {
        StringBuilder report = new StringBuilder();
        
        // Статистика исследования
        ExplorationStats explorationStats = exploration.getExplorationStats();
        report.append("=== ОТЧЕТ О ИССЛЕДОВАНИИ ===\n");
        report.append(explorationStats.getReport()).append("\n");
        
        // Статистика путевых точек
        report.append("=== НАВИГАЦИЯ ===\n");
        report.append("Путевых точек: ").append(waypoints.getAllWaypoints().size()).append("\n");
        report.append("Дорог: ").append(waypoints.getAllRoads().size()).append("\n");
        report.append("Телепортационных хабов: ").append(waypoints.getAllTeleportHubs().size()).append("\n");
        
        // Статистика карт
        MapStats mapStats = maps.getMapStats();
        report.append("=== КАРТЫ ===\n");
        report.append("Всего карт: ").append(mapStats.getTotalMaps()).append("\n");
        report.append("Маркеров: ").append(mapStats.getTotalMarkers()).append("\n");
        report.append("Исследованная площадь: ").append(mapStats.getExploredArea()).append(" блоков²\n");
        
        return report.toString();
    }
    
    /**
     * Экспортировать карту области в текстовом формате
     */
    public String exportAreaMap(BlockPos center, int radius) {
        try {
            SharedMap map = maps.createSharedMap("Export_" + System.currentTimeMillis(), center, radius);
            return maps.exportMapAsText(map.getMapId());
        } catch (Exception e) {
            return "Ошибка при экспорте карты: " + e.getMessage();
        }
    }
    
    /**
     * Сохранить все данные системы
     */
    public void saveAllData() {
        try {
            exploration.saveAllData();
            waypoints.saveAllData();
            maps.saveAllData();
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении данных: " + e.getMessage());
        }
    }
    
    /**
     * Загрузить все данные системы
     */
    public void loadAllData() {
        try {
            exploration.loadAllData();
            waypoints.loadAllData();
            maps.loadAllData();
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке данных: " + e.getMessage());
        }
    }
    
    // Вспомогательные методы
    
    private void createResourceWaypoints(ExplorationResult result) {
        for (ResourceLocation resource : result.getNewResources()) {
            // Создавать путевые точки только для ценных ресурсов
            if (isValuableResource(resource.getResourceType())) {
                String waypointName = resource.getResourceType() + "_" + System.currentTimeMillis();
                waypoints.createWaypoint(
                    waypointName,
                    resource.getPosition(),
                    WaypointType.RESOURCE_SITE,
                    resource.getResourceType() + " (количество: " + resource.getQuantity() + ")"
                );
            }
        }
    }
    
    private boolean isValuableResource(String resourceType) {
        return resourceType.contains("diamond") || 
               resourceType.contains("emerald") || 
               resourceType.contains("gold") || 
               resourceType.contains("iron") ||
               resourceType.contains("ancient_debris");
    }
    
    private double calculateDistance(BlockPos pos1, BlockPos pos2) {
        return Math.sqrt(
            Math.pow(pos1.getX() - pos2.getX(), 2) +
            Math.pow(pos1.getY() - pos2.getY(), 2) +
            Math.pow(pos1.getZ() - pos2.getZ(), 2)
        );
    }
    
    // Геттеры для прямого доступа к системам
    
    public ExplorationSystem getExplorationSystem() {
        return exploration;
    }
    
    public WaypointSystem getWaypointSystem() {
        return waypoints;
    }
    
    public MapSystem getMapSystem() {
        return maps;
    }
}