package com.crafto.ai.command;

import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.exploration.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Команды для управления системой исследования и навигации
 */
public class ExplorationCommands {
    
    /**
     * Команда исследования области
     * Использование: /crafto explore <radius>
     */
    public static void exploreArea(Player player, CraftoEntity crafto, int radius) {
        BlockPos playerPos = player.blockPosition();
        ExplorationSystem exploration = crafto.getExplorationSystem();
        
        player.sendSystemMessage(Component.literal("§6[Crafto] Начинаю исследование области радиусом " + radius + " блоков..."));
        
        CompletableFuture<ExplorationResult> future = exploration.exploreArea(playerPos, radius);
        
        future.thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§a[Crafto] Исследование завершено!"));
                player.sendSystemMessage(Component.literal("§7Найдено областей: " + result.getExploredAreas().size()));
                player.sendSystemMessage(Component.literal("§7Найдено ресурсов: " + result.getNewResources().size()));
                player.sendSystemMessage(Component.literal("§7Опасных зон: " + result.getNewDangerZones().size()));
                
                // Обновляем карту
                for (ExploredArea area : result.getExploredAreas()) {
                    crafto.getMapSystem().updateMapFromExploration(area);
                }
            } else {
                player.sendSystemMessage(Component.literal("§c[Crafto] Ошибка исследования: " + result.getErrorMessage()));
            }
        });
    }
    
    /**
     * Команда создания путевой точки
     * Использование: /crafto waypoint create <name> <type>
     */
    public static void createWaypoint(Player player, CraftoEntity crafto, String name, String type) {
        BlockPos playerPos = player.blockPosition();
        WaypointSystem waypoints = crafto.getWaypointSystem();
        
        try {
            WaypointType waypointType = WaypointType.valueOf(type.toUpperCase());
            Waypoint waypoint = waypoints.createWaypoint(name, playerPos, waypointType, 
                "Создано игроком " + player.getName().getString());
            
            player.sendSystemMessage(Component.literal("§a[Crafto] Путевая точка '" + name + "' создана!"));
            player.sendSystemMessage(Component.literal("§7Тип: " + waypointType.getDisplayName()));
            player.sendSystemMessage(Component.literal("§7Позиция: " + formatPosition(playerPos)));
            
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("§c[Crafto] Ошибка: " + e.getMessage()));
            player.sendSystemMessage(Component.literal("§7Доступные типы: BASE, RESOURCE_SITE, LANDMARK, FARM, MINE"));
        }
    }
    
    /**
     * Команда поиска пути
     * Использование: /crafto navigate <waypoint_name>
     */
    public static void navigateToWaypoint(Player player, CraftoEntity crafto, String waypointName) {
        BlockPos playerPos = player.blockPosition();
        WaypointSystem waypoints = crafto.getWaypointSystem();
        
        Optional<Waypoint> waypoint = waypoints.getWaypoint(waypointName);
        if (waypoint.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[Crafto] Путевая точка '" + waypointName + "' не найдена"));
            return;
        }
        
        Optional<NavigationPath> path = waypoints.findPath(playerPos, waypoint.get().getPosition());
        if (path.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[Crafto] Не удалось найти путь к '" + waypointName + "'"));
            return;
        }
        
        NavigationPath navPath = path.get();
        player.sendSystemMessage(Component.literal("§a[Crafto] Путь к '" + waypointName + "' найден!"));
        player.sendSystemMessage(Component.literal("§7Расстояние: " + String.format("%.1f", navPath.getTotalDistance()) + " блоков"));
        player.sendSystemMessage(Component.literal("§7Время в пути: " + formatTime(navPath.getEstimatedTravelTime())));
        
        // Показываем первые несколько инструкций
        List<String> instructions = navPath.getInstructions();
        player.sendSystemMessage(Component.literal("§6Инструкции:"));
        for (int i = 0; i < Math.min(3, instructions.size()); i++) {
            player.sendSystemMessage(Component.literal("§7" + (i + 1) + ". " + instructions.get(i)));
        }
        
        if (instructions.size() > 3) {
            player.sendSystemMessage(Component.literal("§7... и еще " + (instructions.size() - 3) + " шагов"));
        }
    }
    
    /**
     * Команда создания дороги
     * Использование: /crafto road create <name> <start_waypoint> <end_waypoint> <type>
     */
    public static void createRoad(Player player, CraftoEntity crafto, String roadName, 
                                 String startWaypoint, String endWaypoint, String roadType) {
        WaypointSystem waypoints = crafto.getWaypointSystem();
        
        try {
            RoadType type = RoadType.valueOf(roadType.toUpperCase());
            Road road = waypoints.createRoad(roadName, startWaypoint, endWaypoint, type);
            
            player.sendSystemMessage(Component.literal("§a[Crafto] Дорога '" + roadName + "' создана!"));
            player.sendSystemMessage(Component.literal("§7Тип: " + type.getDisplayName()));
            player.sendSystemMessage(Component.literal("§7Длина: " + String.format("%.1f", road.getDistance()) + " блоков"));
            player.sendSystemMessage(Component.literal("§7Время в пути: " + formatTime(road.getTravelTime())));
            
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("§c[Crafto] Ошибка: " + e.getMessage()));
            player.sendSystemMessage(Component.literal("§7Доступные типы дорог: DIRT_PATH, STONE_ROAD, NETHER_HIGHWAY"));
        }
    }
    
    /**
     * Команда создания телепортационного хаба
     * Использование: /crafto teleport create <name>
     */
    public static void createTeleportHub(Player player, CraftoEntity crafto, String hubName) {
        BlockPos playerPos = player.blockPosition();
        WaypointSystem waypoints = crafto.getWaypointSystem();
        
        try {
            String hubId = "hub_" + hubName.toLowerCase().replaceAll("[^a-z0-9]", "_");
            TeleportHub hub = waypoints.createTeleportHub(hubId, playerPos, hubName);
            
            player.sendSystemMessage(Component.literal("§a[Crafto] Телепортационный хаб '" + hubName + "' создан!"));
            player.sendSystemMessage(Component.literal("§7ID: " + hubId));
            player.sendSystemMessage(Component.literal("§7Позиция: " + formatPosition(playerPos)));
            player.sendSystemMessage(Component.literal("§7Тип: " + hub.getHubType()));
            
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("§c[Crafto] Ошибка: " + e.getMessage()));
        }
    }
    
    /**
     * Команда получения статистики исследования
     * Использование: /crafto stats exploration
     */
    public static void showExplorationStats(Player player, CraftoEntity crafto) {
        ExplorationSystem exploration = crafto.getExplorationSystem();
        ExplorationStats stats = exploration.getExplorationStats();
        
        player.sendSystemMessage(Component.literal("§6=== СТАТИСТИКА ИССЛЕДОВАНИЯ ==="));
        player.sendSystemMessage(Component.literal("§7Исследовано областей: §f" + stats.getTotalExploredAreas()));
        player.sendSystemMessage(Component.literal("§7Найдено ресурсов: §f" + stats.getTotalResources()));
        player.sendSystemMessage(Component.literal("§7Опасных зон: §f" + stats.getDangerousAreas()));
        player.sendSystemMessage(Component.literal("§7Эффективность: §f" + String.format("%.2f", stats.getExplorationEfficiency()) + " ресурсов/область"));
    }
    
    /**
     * Команда получения статистики навигации
     * Использование: /crafto stats navigation
     */
    public static void showNavigationStats(Player player, CraftoEntity crafto) {
        WaypointSystem waypoints = crafto.getWaypointSystem();
        NavigationStats stats = waypoints.getNavigationStats();
        
        player.sendSystemMessage(Component.literal("§6=== СТАТИСТИКА НАВИГАЦИИ ==="));
        player.sendSystemMessage(Component.literal("§7Путевых точек: §f" + stats.getActiveWaypoints() + "/" + stats.getTotalWaypoints()));
        player.sendSystemMessage(Component.literal("§7Дорог: §f" + stats.getActiveRoads() + "/" + stats.getTotalRoads()));
        player.sendSystemMessage(Component.literal("§7Телепорт хабов: §f" + stats.getTotalTeleportHubs()));
        player.sendSystemMessage(Component.literal("§7Общая длина дорог: §f" + String.format("%.1f", stats.getTotalRoadDistance()) + " блоков"));
        player.sendSystemMessage(Component.literal("§7Качество сети: §f" + stats.getNetworkQuality()));
    }
    
    /**
     * Команда экспорта карты
     * Использование: /crafto map export <name> <radius>
     */
    public static void exportMap(Player player, CraftoEntity crafto, String mapName, int radius) {
        BlockPos playerPos = player.blockPosition();
        MapSystem maps = crafto.getMapSystem();
        
        try {
            SharedMap sharedMap = maps.createSharedMap(mapName, playerPos, radius);
            String mapData = maps.exportMapAsText(sharedMap.getMapId());
            
            player.sendSystemMessage(Component.literal("§a[Crafto] Карта '" + mapName + "' создана и экспортирована!"));
            player.sendSystemMessage(Component.literal("§7ID карты: " + sharedMap.getMapId()));
            player.sendSystemMessage(Component.literal("§7Центр: " + formatPosition(playerPos)));
            player.sendSystemMessage(Component.literal("§7Радиус: " + radius + " блоков"));
            
            // В реальной реализации здесь можно сохранить карту в файл или отправить игроку
            System.out.println("=== ЭКСПОРТ КАРТЫ ===");
            System.out.println(mapData);
            
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("§c[Crafto] Ошибка: " + e.getMessage()));
        }
    }
    
    /**
     * Команда поиска ресурсов
     * Использование: /crafto find <resource_type>
     */
    public static void findResources(Player player, CraftoEntity crafto, String resourceType) {
        ExplorationSystem exploration = crafto.getExplorationSystem();
        List<ResourceLocation> resources = exploration.getResourceLocations(resourceType);
        
        if (resources.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[Crafto] Ресурсы типа '" + resourceType + "' не найдены"));
            return;
        }
        
        BlockPos playerPos = player.blockPosition();
        player.sendSystemMessage(Component.literal("§a[Crafto] Найдено " + resources.size() + " месторождений '" + resourceType + "':"));
        
        // Сортируем по расстоянию и показываем ближайшие 5
        resources.stream()
                .filter(r -> !r.isExtracted())
                .sorted((a, b) -> Double.compare(a.getDistanceFrom(playerPos), b.getDistanceFrom(playerPos)))
                .limit(5)
                .forEach(resource -> {
                    double distance = resource.getDistanceFrom(playerPos);
                    player.sendSystemMessage(Component.literal(String.format("§7- %s (%.1f блоков, ценность: %d)", 
                        formatPosition(resource.getPosition()), distance, resource.getValue())));
                });
        
        if (resources.size() > 5) {
            player.sendSystemMessage(Component.literal("§7... и еще " + (resources.size() - 5) + " месторождений"));
        }
    }
    
    // Вспомогательные методы
    
    private static String formatPosition(BlockPos pos) {
        return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }
    
    private static String formatTime(int seconds) {
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
}