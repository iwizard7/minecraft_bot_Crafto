package examples;

import com.crafto.ai.exploration.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Пример использования системы исследования и навигации
 * Демонстрирует основные возможности и сценарии использования
 */
public class ExplorationExample {
    
    public static void main(String[] args) {
        // Примечание: В реальном использовании Level world должен быть получен из Minecraft
        Level world = null; // Заменить на реальный мир
        String craftoName = "ExplorerBot";
        
        // Создать интеграционный класс
        ExplorationIntegration explorer = new ExplorationIntegration(world, craftoName);
        
        // Загрузить существующие данные
        explorer.loadAllData();
        
        // Демонстрация различных сценариев
        demonstrateQuickStart(explorer);
        demonstrateAutoExploration(explorer);
        demonstrateFastTravel(explorer);
        demonstrateResourceFinding(explorer);
        demonstrateReporting(explorer);
    }
    
    /**
     * Демонстрация быстрого старта
     */
    private static void demonstrateQuickStart(ExplorationIntegration explorer) {
        System.out.println("=== ДЕМОНСТРАЦИЯ БЫСТРОГО СТАРТА ===");
        
        BlockPos startPos = new BlockPos(0, 64, 0);
        int radius = 128;
        
        CompletableFuture<Void> quickStart = explorer.quickStart(startPos, radius);
        
        // Ждем завершения (в реальном коде лучше использовать callback'и)
        quickStart.join();
        
        System.out.println("Быстрый старт завершен!\n");
    }
    
    /**
     * Демонстрация автоматического исследования
     */
    private static void demonstrateAutoExploration(ExplorationIntegration explorer) {
        System.out.println("=== ДЕМОНСТРАЦИЯ АВТОМАТИЧЕСКОГО ИССЛЕДОВАНИЯ ===");
        
        BlockPos center = new BlockPos(0, 64, 0);
        int totalRadius = 500;
        
        System.out.println("Начинаем исследование области радиусом " + totalRadius + " блоков...");
        
        CompletableFuture<Void> autoExplore = explorer.autoExplore(center, totalRadius);
        
        // В реальном коде это должно выполняться асинхронно
        // autoExplore.join();
        
        System.out.println("Автоматическое исследование запущено!\n");
    }
    
    /**
     * Демонстрация системы быстрого перемещения
     */
    private static void demonstrateFastTravel(ExplorationIntegration explorer) {
        System.out.println("=== ДЕМОНСТРАЦИЯ БЫСТРОГО ПЕРЕМЕЩЕНИЯ ===");
        
        // Создать несколько важных точек
        WaypointSystem waypoints = explorer.getWaypointSystem();
        
        waypoints.createWaypoint("MainBase", new BlockPos(0, 64, 0), WaypointType.BASE, "Главная база");
        waypoints.createWaypoint("DiamondMine", new BlockPos(200, 30, 150), WaypointType.MINE, "Алмазная шахта");
        waypoints.createWaypoint("TradingPost", new BlockPos(-100, 64, 200), WaypointType.TRADING_POST, "Торговый пост");
        
        // Настроить систему быстрого перемещения
        BlockPos hubPos = new BlockPos(0, 100, 0);
        explorer.setupFastTravel(hubPos);
        
        // Найти путь между точками
        Optional<NavigationPath> path = waypoints.findPath(
            new BlockPos(0, 64, 0),      // от базы
            new BlockPos(200, 30, 150)   // к шахте
        );
        
        if (path.isPresent()) {
            NavigationPath route = path.get();
            System.out.println("Найден путь к шахте:");
            System.out.println("Расстояние: " + route.getTotalDistance() + " блоков");
            System.out.println("Время в пути: " + route.getEstimatedTime() + " секунд");
        }
        
        System.out.println("Система быстрого перемещения настроена!\n");
    }
    
    /**
     * Демонстрация поиска ресурсов
     */
    private static void demonstrateResourceFinding(ExplorationIntegration explorer) {
        System.out.println("=== ДЕМОНСТРАЦИЯ ПОИСКА РЕСУРСОВ ===");
        
        BlockPos currentPos = new BlockPos(50, 64, 50);
        
        // Найти ближайшие алмазы
        Optional<NavigationPath> pathToDiamonds = explorer.findNearestResource(currentPos, "diamond_ore");
        
        if (pathToDiamonds.isPresent()) {
            NavigationPath route = pathToDiamonds.get();
            System.out.println("Найден путь к ближайшим алмазам:");
            System.out.println("Расстояние: " + route.getTotalDistance() + " блоков");
            System.out.println("Время в пути: " + route.getEstimatedTime() + " секунд");
        } else {
            System.out.println("Алмазы не найдены в исследованных областях");
        }
        
        // Получить все найденные ресурсы
        ExplorationSystem exploration = explorer.getExplorationSystem();
        System.out.println("Найденные ресурсы:");
        System.out.println("- Железо: " + exploration.getResourceLocations("iron_ore").size() + " месторождений");
        System.out.println("- Золото: " + exploration.getResourceLocations("gold_ore").size() + " месторождений");
        System.out.println("- Алмазы: " + exploration.getResourceLocations("diamond_ore").size() + " месторождений");
        
        System.out.println();
    }
    
    /**
     * Демонстрация отчетности
     */
    private static void demonstrateReporting(ExplorationIntegration explorer) {
        System.out.println("=== ДЕМОНСТРАЦИЯ ОТЧЕТНОСТИ ===");
        
        // Получить полный отчет
        String report = explorer.getExplorationReport();
        System.out.println(report);
        
        // Экспортировать карту области
        BlockPos mapCenter = new BlockPos(0, 64, 0);
        String mapText = explorer.exportAreaMap(mapCenter, 200);
        System.out.println("Карта области (200 блоков):");
        System.out.println(mapText);
        
        // Сохранить все данные
        explorer.saveAllData();
        System.out.println("Все данные сохранены!");
    }
    
    /**
     * Пример интеграции с основным циклом Crafto AI
     */
    public static class CraftoAIIntegration {
        private ExplorationIntegration explorer;
        private boolean isExploring = false;
        
        public CraftoAIIntegration(Level world, String craftoName) {
            this.explorer = new ExplorationIntegration(world, craftoName);
            this.explorer.loadAllData();
        }
        
        /**
         * Метод, который можно вызывать из основного цикла Crafto AI
         */
        public void onTick(BlockPos currentPos) {
            // Проверить, нужно ли исследовать новые области
            if (!isExploring && shouldExploreMore(currentPos)) {
                startExploration(currentPos);
            }
            
            // Автоматически создавать путевые точки для важных мест
            autoCreateWaypoints(currentPos);
            
            // Периодически сохранять данные
            if (System.currentTimeMillis() % 60000 == 0) { // каждую минуту
                explorer.saveAllData();
            }
        }
        
        private boolean shouldExploreMore(BlockPos pos) {
            // Логика определения необходимости исследования
            ExplorationSystem exploration = explorer.getExplorationSystem();
            ChunkCoordinate currentChunk = new ChunkCoordinate(pos.getX() >> 4, pos.getZ() >> 4);
            
            // Если текущий чанк не исследован, нужно исследовать
            return !exploration.getExploredArea(currentChunk).isPresent();
        }
        
        private void startExploration(BlockPos pos) {
            isExploring = true;
            explorer.autoExplore(pos, 200).thenRun(() -> {
                isExploring = false;
                System.out.println("Исследование области завершено");
            });
        }
        
        private void autoCreateWaypoints(BlockPos pos) {
            // Автоматически создавать путевые точки для интересных мест
            // Например, если нашли деревню, создать путевую точку
            // Это зависит от конкретной логики обнаружения структур
        }
        
        /**
         * Получить рекомендации для следующих действий
         */
        public String getRecommendations(BlockPos currentPos) {
            StringBuilder recommendations = new StringBuilder();
            
            // Рекомендации по исследованию
            if (!isExploring) {
                recommendations.append("- Исследовать область вокруг текущей позиции\n");
            }
            
            // Рекомендации по ресурсам
            Optional<NavigationPath> pathToDiamonds = explorer.findNearestResource(currentPos, "diamond_ore");
            if (pathToDiamonds.isPresent()) {
                recommendations.append("- Добыть алмазы в ").append(pathToDiamonds.get().getTotalDistance())
                    .append(" блоках отсюда\n");
            }
            
            // Рекомендации по строительству
            WaypointSystem waypoints = explorer.getWaypointSystem();
            if (waypoints.getAllWaypoints().size() > 2 && waypoints.getAllRoads().isEmpty()) {
                recommendations.append("- Построить дороги между путевыми точками\n");
            }
            
            return recommendations.toString();
        }
    }
}