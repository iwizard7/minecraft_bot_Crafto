# Система исследования и навигации

Комплексная система для автоматического исследования мира, создания карт и навигации в Minecraft.

## Обзор компонентов

### 1. ExplorationSystem - Система исследования

Основной класс для автоматического исследования территории.

**Основные возможности:**
- Картографирование неизвестных областей по чанкам
- Автоматический поиск и каталогизация ресурсов
- Обнаружение и оценка уровня опасности
- Создание торговых маршрутов между важными точками
- Планирование и приоритизация исследований

**Пример использования:**
```java
ExplorationSystem exploration = new ExplorationSystem(world, "MyCrafto");

// Исследовать область радиусом 64 блока
BlockPos center = new BlockPos(0, 64, 0);
CompletableFuture<ExplorationResult> future = exploration.exploreArea(center, 64);
ExplorationResult result = future.join();

// Получить статистику
ExplorationStats stats = exploration.getExplorationStats();
System.out.println(stats.getReport());
```

### 2. WaypointSystem - Навигационная система

Система управления путевыми точками и дорогами.

**Основные возможности:**
- Создание и управление путевыми точками разных типов
- Автоматическое размещение маяков в важных местах
- Построение сети дорог между базами
- Телепортационные хабы для быстрого перемещения
- Поиск оптимальных маршрутов (алгоритм Дейкстры)

**Пример использования:**
```java
WaypointSystem waypoints = new WaypointSystem(world, "MyCrafto");

// Создать базовую путевую точку
BlockPos basePos = new BlockPos(100, 64, 200);
Waypoint base = waypoints.createWaypoint("MainBase", basePos, WaypointType.BASE, "Главная база");

// Создать дорогу между точками
Road road = waypoints.createRoad("road1", "MainBase", "ResourceSite", RoadType.STONE_ROAD);

// Найти путь между точками
Optional<NavigationPath> path = waypoints.findPath(startPos, endPos);
```

### 3. MapSystem - Система карт

Автоматическое создание и экспорт карт исследованных областей.

**Основные возможности:**
- Автоматическое создание карт на основе исследований
- Размещение маркеров для ресурсов и структур
- Создание совместных карт для обмена между игроками
- Экспорт карт в текстовый и JSON форматы

**Пример использования:**
```java
MapSystem maps = new MapSystem(world, "MyCrafto", exploration, waypoints);

// Создать маркер
BlockPos resourcePos = new BlockPos(50, 30, 75);
MapMarker marker = maps.createCustomMarker("DiamondMine", resourcePos, 
    MarkerType.RESOURCE, "Богатое месторождение алмазов");

// Создать совместную карту
SharedMap sharedMap = maps.createSharedMap("MyArea", center, 500);

// Экспортировать карту
String mapData = maps.exportMapAsText(sharedMap.getMapId());
```

## Типы данных

### Путевые точки (WaypointType)
- **BASE** - Базы и поселения
- **RESOURCE_SITE** - Месторождения ресурсов
- **TELEPORT_HUB** - Телепортационные хабы
- **LANDMARK** - Ориентиры
- **DANGER_ZONE** - Опасные зоны
- **TRADING_POST** - Торговые посты
- **FARM** - Фермы
- **MINE** - Шахты
- **PORTAL** - Порталы

### Типы дорог (RoadType)
- **DIRT_PATH** - Грунтовые тропы (скорость x1.0)
- **STONE_ROAD** - Каменные дороги (скорость x1.2)
- **NETHER_HIGHWAY** - Незер магистрали (скорость x8.0)
- **WATER_CANAL** - Водные каналы (скорость x0.8)
- **RAIL_TRACK** - Железные дороги (скорость x2.0)
- **ICE_ROAD** - Ледяные дороги (скорость x1.5)

### Маркеры карты (MarkerType)
- **RESOURCE** - Ресурсы
- **STRUCTURE** - Структуры
- **BASE** - Базы
- **WAYPOINT** - Путевые точки
- **DANGER** - Опасности
- **TELEPORT_HUB** - Телепорты
- **LANDMARK** - Ориентиры

## Файловая структура

Система сохраняет данные в следующих директориях:

```
config/crafto/
├── exploration/
│   ├── explored_areas.json      # Исследованные области
│   ├── resource_locations.json  # Местоположения ресурсов
│   ├── danger_zones.json        # Опасные зоны
│   └── trade_routes.json        # Торговые маршруты
├── waypoints/
│   ├── waypoints.json           # Путевые точки
│   ├── roads.json               # Дороги
│   └── teleport_hubs.json       # Телепортационные хабы
└── maps/
    ├── world_map.json           # Карта мира
    ├── markers.json             # Маркеры
    └── shared_maps.json         # Совместные карты
```

## Интеграция с другими системами

### С LongTermMemory
```java
// Сохранение успешных стратегий исследования
longTermMemory.recordSuccessfulStrategy("EXPLORATION", "plains_biome", 
    "systematic_spiral_search", 0.85);

// Запись поведения при навигации
longTermMemory.recordPlayerBehavior(playerName, "use_waypoint", "fast_travel", true);
```

### С TaskPlanner
```java
// Создание задач исследования
taskPlanner.addTask("explore_area", center, radius);
taskPlanner.addTask("create_waypoint", position, waypointType);
taskPlanner.addTask("build_road", startWaypoint, endWaypoint);
```

## Производительность

- **Исследование**: ~1-2 секунды на чанк
- **Поиск пути**: <100мс для сетей до 1000 узлов
- **Сохранение данных**: Автоматическое при изменениях
- **Память**: ~500 байт на чанк, ~200 байт на маркер

## Конфигурация

Основные параметры можно настроить в коде:

```java
// ExplorationSystem
private static final int EXPLORATION_RADIUS = 64;
private static final int MAX_EXPLORATION_DISTANCE = 1000;
private static final int DANGER_ASSESSMENT_RADIUS = 32;

// WaypointSystem - автоматически определяется типом

// MapSystem - автоматически определяется на основе данных
```

## Примеры использования

### Автоматическое исследование области
```java
ExplorationSystem exploration = new ExplorationSystem(world, "AutoExplorer");

// Запланировать исследование в радиусе 1000 блоков
BlockPos currentPos = craftoEntity.getPosition();
exploration.scheduleExploration(currentPos, 1000);

// Выполнить следующую задачу исследования
Optional<ExplorationTask> task = exploration.getNextExplorationTask();
if (task.isPresent()) {
    ChunkCoordinate coord = task.get().getCoordinate();
    BlockPos center = new BlockPos(coord.x << 4, 64, coord.z << 4);
    exploration.exploreArea(center, 64);
}
```

### Создание навигационной сети
```java
WaypointSystem waypoints = new WaypointSystem(world, "Navigator");

// Создать основные точки
Waypoint spawn = waypoints.createWaypoint("Spawn", spawnPos, WaypointType.BASE);
Waypoint mine = waypoints.createWaypoint("DiamondMine", minePos, WaypointType.MINE);
Waypoint farm = waypoints.createWaypoint("WheatFarm", farmPos, WaypointType.FARM);

// Соединить дорогами
waypoints.createRoad("spawn_to_mine", "Spawn", "DiamondMine", RoadType.STONE_ROAD);
waypoints.createRoad("spawn_to_farm", "Spawn", "WheatFarm", RoadType.DIRT_PATH);

// Создать телепортационный хаб
TeleportHub hub = waypoints.createTeleportHub("central_hub", hubPos, "Центральный хаб");
```

### Экспорт карты для игроков
```java
MapSystem maps = new MapSystem(world, "Cartographer", exploration, waypoints);

// Создать карту области вокруг базы
BlockPos baseCenter = new BlockPos(0, 64, 0);
SharedMap baseMap = maps.createSharedMap("BaseArea", baseCenter, 1000);

// Экспортировать в текстовый формат
String textMap = maps.exportMapAsText(baseMap.getMapId());
System.out.println(textMap);

// Экспортировать в JSON для веб-интерфейса
String jsonMap = maps.exportMapAsJson(baseMap.getMapId());
```

## Расширение системы

Система спроектирована для легкого расширения:

1. **Новые типы путевых точек**: Добавить в enum WaypointType
2. **Новые типы дорог**: Добавить в enum RoadType с соответствующими параметрами
3. **Новые алгоритмы поиска пути**: Реализовать в WaypointSystem
4. **Новые форматы экспорта**: Добавить методы в MapSystem
5. **Интеграция с внешними системами**: Использовать события и callback'и

Система готова к использованию и интеграции с остальными компонентами Crafto AI.