# Руководство по интеграции системы исследования с Crafto AI

## Быстрая интеграция

### 1. Добавление в основной класс Crafto

```java
public class CraftoAI {
    private ExplorationIntegration explorer;
    
    public void initialize(Level world, String craftoName) {
        // Инициализация системы исследования
        this.explorer = new ExplorationIntegration(world, craftoName);
        this.explorer.loadAllData();
        
        // Быстрый старт при первом запуске
        BlockPos spawnPos = getSpawnPosition();
        explorer.quickStart(spawnPos, 128);
    }
    
    public void onTick() {
        BlockPos currentPos = getCurrentPosition();
        
        // Автоматическое исследование неизвестных областей
        if (shouldExplore(currentPos)) {
            explorer.autoExplore(currentPos, 200);
        }
        
        // Периодическое сохранение данных
        if (tickCounter % 1200 == 0) { // каждую минуту
            explorer.saveAllData();
        }
    }
}
```

### 2. Команды для игроков

```java
public class CraftoCommands {
    
    @Command("explore")
    public void exploreCommand(Player player, int radius) {
        BlockPos playerPos = player.blockPosition();
        explorer.autoExplore(playerPos, radius).thenRun(() -> {
            player.sendMessage("Исследование завершено!");
        });
    }
    
    @Command("waypoint")
    public void waypointCommand(Player player, String name, String type) {
        BlockPos pos = player.blockPosition();
        WaypointType waypointType = WaypointType.valueOf(type.toUpperCase());
        
        explorer.getWaypointSystem().createWaypoint(name, pos, waypointType, "Создано игроком");
        player.sendMessage("Путевая точка '" + name + "' создана!");
    }
    
    @Command("navigate")
    public void navigateCommand(Player player, String waypointName) {
        Optional<NavigationPath> path = explorer.getWaypointSystem()
            .findPath(player.blockPosition(), getWaypointPosition(waypointName));
            
        if (path.isPresent()) {
            // Показать путь игроку (частицы, указатели и т.д.)
            showPathToPlayer(player, path.get());
        } else {
            player.sendMessage("Путь не найден!");
        }
    }
    
    @Command("map")
    public void mapCommand(Player player, int radius) {
        String mapText = explorer.exportAreaMap(player.blockPosition(), radius);
        player.sendMessage(mapText);
    }
    
    @Command("report")
    public void reportCommand(Player player) {
        String report = explorer.getExplorationReport();
        player.sendMessage(report);
    }
}
```

### 3. Интеграция с планировщиком задач

```java
public class TaskPlannerIntegration {
    
    public void addExplorationTasks(HierarchicalTaskPlanner planner) {
        // Задача исследования
        planner.addTask("explore_area", (context) -> {
            BlockPos center = context.getBlockPos("center");
            int radius = context.getInt("radius");
            return explorer.autoExplore(center, radius);
        });
        
        // Задача создания путевой точки
        planner.addTask("create_waypoint", (context) -> {
            String name = context.getString("name");
            BlockPos pos = context.getBlockPos("position");
            WaypointType type = context.getEnum("type", WaypointType.class);
            
            explorer.getWaypointSystem().createWaypoint(name, pos, type, "Автоматически создано");
            return CompletableFuture.completedFuture(null);
        });
        
        // Задача поиска ресурсов
        planner.addTask("find_resource", (context) -> {
            String resourceType = context.getString("resource_type");
            BlockPos from = context.getBlockPos("from");
            
            Optional<NavigationPath> path = explorer.findNearestResource(from, resourceType);
            context.setResult("path", path);
            return CompletableFuture.completedFuture(null);
        });
    }
}
```

### 4. Интеграция с долгосрочной памятью

```java
public class LongTermMemoryIntegration {
    
    public void recordExplorationSuccess(String biome, String strategy, double successRate) {
        longTermMemory.recordSuccessfulStrategy("EXPLORATION", biome, strategy, successRate);
    }
    
    public void recordPlayerPreferences(String playerName, WaypointType preferredType) {
        longTermMemory.recordPlayerBehavior(playerName, "preferred_waypoint_type", 
            preferredType.name(), true);
    }
    
    public void adaptExplorationStrategy(String biome) {
        Optional<String> bestStrategy = longTermMemory.getBestStrategy("EXPLORATION", biome);
        if (bestStrategy.isPresent()) {
            // Применить лучшую стратегию для данного биома
            applyExplorationStrategy(bestStrategy.get());
        }
    }
}
```

## Автоматические сценарии

### 1. Автоматический разведчик

```java
public class AutoScout {
    private final ExplorationIntegration explorer;
    private final Timer timer = new Timer();
    
    public void startAutoScouting(BlockPos basePos) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Найти неисследованные области рядом с базой
                List<ChunkCoordinate> unexplored = findUnexploredNearby(basePos, 500);
                
                if (!unexplored.isEmpty()) {
                    ChunkCoordinate target = unexplored.get(0);
                    BlockPos centerPos = new BlockPos(target.x << 4, 64, target.z << 4);
                    
                    explorer.getExplorationSystem().exploreArea(centerPos, 64).thenRun(() -> {
                        System.out.println("Автоматически исследован чанк: " + target);
                    });
                }
            }
        }, 0, 30000); // каждые 30 секунд
    }
}
```

### 2. Система торговых маршрутов

```java
public class TradeRouteManager {
    
    public void setupTradeNetwork() {
        WaypointSystem waypoints = explorer.getWaypointSystem();
        
        // Найти все торговые посты
        List<Waypoint> tradePosts = waypoints.getWaypointsByType(WaypointType.TRADING_POST);
        
        // Создать оптимальную сеть дорог
        for (int i = 0; i < tradePosts.size(); i++) {
            for (int j = i + 1; j < tradePosts.size(); j++) {
                Waypoint from = tradePosts.get(i);
                Waypoint to = tradePosts.get(j);
                
                double distance = calculateDistance(from.getPosition(), to.getPosition());
                if (distance < 1000) { // только для близких точек
                    waypoints.createRoad(
                        "trade_" + from.getName() + "_" + to.getName(),
                        from.getName(),
                        to.getName(),
                        RoadType.STONE_ROAD
                    );
                }
            }
        }
    }
    
    public List<BlockPos> planTradeRoute(List<String> destinations) {
        // Решение задачи коммивояжера для оптимального маршрута
        return explorer.getWaypointSystem().findOptimalRoute(destinations);
    }
}
```

### 3. Система безопасности

```java
public class SecuritySystem {
    
    public void markDangerousAreas() {
        ExplorationSystem exploration = explorer.getExplorationSystem();
        
        // Найти все опасные зоны
        Map<ChunkCoordinate, DangerZone> dangerZones = exploration.getAllDangerZones();
        
        for (DangerZone zone : dangerZones.values()) {
            if (zone.getDangerLevel() > 0.7) { // высокий уровень опасности
                // Создать предупреждающую путевую точку
                explorer.getWaypointSystem().createWaypoint(
                    "DANGER_" + zone.getCoordinate().toString(),
                    new BlockPos(zone.getCoordinate().x << 4, 64, zone.getCoordinate().z << 4),
                    WaypointType.DANGER_ZONE,
                    "Опасная зона! Уровень: " + zone.getDangerLevel()
                );
            }
        }
    }
    
    public boolean isSafeToTravel(BlockPos from, BlockPos to) {
        Optional<NavigationPath> path = explorer.getWaypointSystem().findPath(from, to);
        
        if (path.isPresent()) {
            // Проверить, проходит ли путь через опасные зоны
            return !pathThroughDanger(path.get());
        }
        
        return false;
    }
}
```

## Конфигурация и настройка

### 1. Файл конфигурации

```json
{
  "exploration": {
    "auto_explore_radius": 200,
    "exploration_interval": 30000,
    "max_concurrent_explorations": 3,
    "valuable_resources": ["diamond_ore", "emerald_ore", "gold_ore", "ancient_debris"]
  },
  "waypoints": {
    "auto_create_for_resources": true,
    "min_resource_quantity": 5,
    "max_waypoints_per_chunk": 3
  },
  "maps": {
    "auto_update_interval": 60000,
    "max_map_size": 2000,
    "export_format": "text"
  }
}
```

### 2. Загрузка конфигурации

```java
public class ExplorationConfig {
    public static ExplorationConfig load(String configPath) {
        // Загрузить конфигурацию из JSON файла
        return gson.fromJson(new FileReader(configPath), ExplorationConfig.class);
    }
    
    public void apply(ExplorationIntegration explorer) {
        // Применить настройки к системе исследования
    }
}
```

## Мониторинг и отладка

### 1. Логирование

```java
public class ExplorationLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExplorationLogger.class);
    
    public void logExplorationStart(BlockPos pos, int radius) {
        LOGGER.info("Начато исследование области: центр={}, радиус={}", pos, radius);
    }
    
    public void logResourceFound(ResourceLocation resource) {
        LOGGER.info("Найден ресурс: тип={}, позиция={}, количество={}", 
            resource.getResourceType(), resource.getPosition(), resource.getQuantity());
    }
    
    public void logWaypointCreated(Waypoint waypoint) {
        LOGGER.info("Создана путевая точка: имя={}, тип={}, позиция={}", 
            waypoint.getName(), waypoint.getType(), waypoint.getPosition());
    }
}
```

### 2. Метрики производительности

```java
public class ExplorationMetrics {
    private final MeterRegistry meterRegistry;
    
    public void recordExplorationTime(Duration duration) {
        Timer.Sample.start(meterRegistry).stop(Timer.builder("exploration.time").register(meterRegistry));
    }
    
    public void incrementResourcesFound(String resourceType) {
        Counter.builder("resources.found")
            .tag("type", resourceType)
            .register(meterRegistry)
            .increment();
    }
}
```

Система готова к интеграции с основным Crafto AI! Начните с простой интеграции и постепенно добавляйте дополнительные функции по мере необходимости.