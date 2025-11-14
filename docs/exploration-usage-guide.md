# Руководство по использованию системы исследования и навигации

## Быстрый старт

Система исследования и навигации состоит из трех основных компонентов, которые работают вместе для автоматического исследования мира Minecraft.

### 1. Базовая настройка

```java
// Инициализация всех систем
Level world = // ваш мир
String craftoName = "MyCrafto";

ExplorationSystem exploration = new ExplorationSystem(world, craftoName);
WaypointSystem waypoints = new WaypointSystem(world, craftoName);
MapSystem maps = new MapSystem(world, craftoName, exploration, waypoints);
```

### 2. Исследование территории

#### Простое исследование области
```java
// Исследовать область радиусом 64 блока вокруг текущей позиции
BlockPos currentPos = new BlockPos(0, 64, 0);
CompletableFuture<ExplorationResult> future = exploration.exploreArea(currentPos, 64);

// Получить результат
ExplorationResult result = future.join();
System.out.println("Исследовано областей: " + result.getExploredAreas().size());
System.out.println("Найдено ресурсов: " + result.getNewResources().size());
```

#### Автоматическое исследование
```java
// Запланировать исследование большой области
exploration.scheduleExploration(currentPos, 500); // радиус 500 блоков

// Выполнять исследование по частям
while (true) {
    Optional<ExplorationTask> task = exploration.getNextExplorationTask();
    if (task.isPresent()) {
        ChunkCoordinate coord = task.get().getCoordinate();
        BlockPos center = new BlockPos(coord.x << 4, 64, coord.z << 4);
        exploration.exploreArea(center, 64).join();
        
        // Небольшая пауза между исследованиями
        Thread.sleep(1000);
    } else {
        break; // Все задачи выполнены
    }
}
```

### 3. Работа с путевыми точками

#### Создание основных точек
```java
// Создать базу
BlockPos basePos = new BlockPos(100, 64, 200);
Waypoint base = waypoints.createWaypoint("MainBase", basePos, WaypointType.BASE, "Главная база");

// Создать шахту
BlockPos minePos = new BlockPos(150, 30, 250);
Waypoint mine = waypoints.createWaypoint("DiamondMine", minePos, WaypointType.MINE, "Алмазная шахта");

// Создать ферму
BlockPos farmPos = new BlockPos(80, 64, 180);
Waypoint farm = waypoints.createWaypoint("WheatFarm", farmPos, WaypointType.FARM, "Пшеничная ферма");
```

#### Создание дорог между точками
```java
// Соединить базу с шахтой каменной дорогой
Road roadToMine = waypoints.createRoad("base_to_mine", "MainBase", "DiamondMine", RoadType.STONE_ROAD);

// Соединить базу с фермой грунтовой тропой
Road roadToFarm = waypoints.createRoad("base_to_farm", "MainBase", "WheatFarm", RoadType.DIRT_PATH);
```

#### Навигация
```java
// Найти путь от базы к шахте
Optional<NavigationPath> path = waypoints.findPath(basePos, minePos);
if (path.isPresent()) {
    NavigationPath route = path.get();
    System.out.println("Расстояние: " + route.getTotalDistance() + " блоков");
    System.out.println("Время в пути: " + route.getEstimatedTime() + " секунд");
    
    // Получить список точек маршрута
    List<BlockPos> waypoints = route.getWaypoints();
    for (BlockPos pos : waypoints) {
        System.out.println("Точка маршрута: " + pos);
    }
}
```

### 4. Работа с картами

#### Создание карты области
```java
// Создать карту области вокруг базы
BlockPos mapCenter = new BlockPos(100, 64, 200);
SharedMap areaMap = maps.createSharedMap("BaseArea", mapCenter, 1000);

// Добавить пользовательские маркеры
BlockPos treasurePos = new BlockPos(120, 40, 180);
MapMarker treasure = maps.createCustomMarker("Treasure", treasurePos, 
    MarkerType.LANDMARK, "Сундук с сокровищами");
```

#### Экспорт карты
```java
// Экспорт в текстовом формате (ASCII карта)
String textMap = maps.exportMapAsText(areaMap.getMapId());
System.out.println(textMap);

// Экспорт в JSON для веб-интерфейса
String jsonMap = maps.exportMapAsJson(areaMap.getMapId());
// Сохранить в файл или отправить по API
```

### 5. Получение статистики

#### Статистика исследования
```java
ExplorationStats stats = exploration.getExplorationStats();
System.out.println("=== Статистика исследования ===");
System.out.println(stats.getReport());
```

#### Статистика карт
```java
MapStats mapStats = maps.getMapStats();
System.out.println("=== Статистика карт ===");
System.out.println("Всего карт: " + mapStats.getTotalMaps());
System.out.println("Исследованная площадь: " + mapStats.getExploredArea() + " блоков²");
System.out.println("Маркеров на картах: " + mapStats.getTotalMarkers());
```

## Практические сценарии использования

### Сценарий 1: Автоматический разведчик

```java
public class AutoExplorer {
    private ExplorationSystem exploration;
    private WaypointSystem waypoints;
    private MapSystem maps;
    
    public void startExploration(BlockPos startPos) {
        // 1. Создать базовую точку
        waypoints.createWaypoint("ExplorationBase", startPos, WaypointType.BASE, "База разведки");
        
        // 2. Запланировать исследование
        exploration.scheduleExploration(startPos, 1000);
        
        // 3. Выполнять исследование
        while (hasMoreToExplore()) {
            Optional<ExplorationTask> task = exploration.getNextExplorationTask();
            if (task.isPresent()) {
                exploreChunk(task.get());
                createWaypointsForResources();
                updateMap();
            }
        }
    }
    
    private void createWaypointsForResources() {
        // Автоматически создавать путевые точки для найденных ресурсов
        List<ResourceLocation> newResources = exploration.getResourceLocations("diamond_ore");
        for (ResourceLocation resource : newResources) {
            if (!waypoints.hasWaypointAt(resource.getPosition())) {
                waypoints.createWaypoint(
                    "Diamond_" + System.currentTimeMillis(),
                    resource.getPosition(),
                    WaypointType.RESOURCE_SITE,
                    "Алмазы: " + resource.getQuantity()
                );
            }
        }
    }
}
```

### Сценарий 2: Система быстрого перемещения

```java
public class FastTravelSystem {
    private WaypointSystem waypoints;
    
    public void setupFastTravel() {
        // Создать центральный телепортационный хаб
        BlockPos hubPos = new BlockPos(0, 100, 0);
        TeleportHub centralHub = waypoints.createTeleportHub("CentralHub", hubPos, "Центральный хаб");
        
        // Добавить важные точки к хабу
        centralHub.addDestination("MainBase");
        centralHub.addDestination("DiamondMine");
        centralHub.addDestination("WheatFarm");
        
        // Создать региональные хабы
        createRegionalHub("NorthHub", new BlockPos(0, 64, -500));
        createRegionalHub("SouthHub", new BlockPos(0, 64, 500));
        createRegionalHub("EastHub", new BlockPos(500, 64, 0));
        createRegionalHub("WestHub", new BlockPos(-500, 64, 0));
    }
    
    public void teleportTo(String waypointName) {
        Optional<Waypoint> waypoint = waypoints.getWaypoint(waypointName);
        if (waypoint.isPresent()) {
            BlockPos destination = waypoint.get().getPosition();
            // Выполнить телепортацию игрока
            teleportPlayer(destination);
        }
    }
}
```

### Сценарий 3: Торговая сеть

```java
public class TradeNetwork {
    private ExplorationSystem exploration;
    private WaypointSystem waypoints;
    
    public void setupTradeRoutes() {
        // Найти все торговые посты
        List<Waypoint> tradePosts = waypoints.getWaypointsByType(WaypointType.TRADING_POST);
        
        // Создать торговые маршруты между постами
        for (int i = 0; i < tradePosts.size(); i++) {
            for (int j = i + 1; j < tradePosts.size(); j++) {
                Waypoint from = tradePosts.get(i);
                Waypoint to = tradePosts.get(j);
                
                // Создать дорогу если расстояние разумное
                double distance = calculateDistance(from.getPosition(), to.getPosition());
                if (distance < 1000) {
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
    
    public List<BlockPos> findOptimalTradeRoute(List<String> destinations) {
        // Найти оптимальный маршрут через все торговые точки
        List<BlockPos> route = new ArrayList<>();
        String current = "MainBase";
        
        for (String destination : destinations) {
            Optional<NavigationPath> path = waypoints.findPath(
                waypoints.getWaypoint(current).get().getPosition(),
                waypoints.getWaypoint(destination).get().getPosition()
            );
            
            if (path.isPresent()) {
                route.addAll(path.get().getWaypoints());
                current = destination;
            }
        }
        
        return route;
    }
}
```

## Советы по оптимизации

### 1. Производительность
- Исследуйте по частям, не более 64 блоков радиуса за раз
- Используйте `scheduleExploration()` для больших областей
- Сохраняйте данные периодически с помощью `saveAllData()`

### 2. Память
- Очищайте старые данные исследования: `exploration.clearOldExplorationData(30)` (30 дней)
- Ограничивайте количество маркеров на карте
- Используйте сжатие для экспорта больших карт

### 3. Навигация
- Создавайте иерархию дорог: основные магистрали + местные тропы
- Используйте телепортационные хабы для дальних расстояний
- Регулярно обновляйте пути при изменении ландшафта

## Интеграция с другими системами

### С LongTermMemory
```java
// Сохранение успешных стратегий исследования
longTermMemory.recordSuccessfulStrategy("EXPLORATION", biome, "spiral_search", successRate);

// Запись предпочтений игрока
longTermMemory.recordPlayerBehavior(playerName, "preferred_waypoint_type", waypointType.name(), true);
```

### С TaskPlanner
```java
// Добавление задач исследования в планировщик
taskPlanner.addTask("explore_area", centerPos, radius, Priority.MEDIUM);
taskPlanner.addTask("create_waypoint_network", basePos, 1000, Priority.HIGH);
taskPlanner.addTask("update_trade_routes", null, 0, Priority.LOW);
```

Система готова к использованию! Начните с простого исследования небольшой области и постепенно расширяйте функциональность.