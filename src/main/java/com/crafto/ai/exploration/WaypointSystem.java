package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Навигационная система с автоматическими маяками и сетью дорог
 * Включает путевые точки, телепортационные хабы и систему быстрого перемещения
 */
public class WaypointSystem {
    
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();
    
    private static final String WAYPOINT_DIR = "config/crafto/waypoints/";
    private static final String WAYPOINTS_FILE = "waypoints.json";
    private static final String ROADS_FILE = "roads.json";
    private static final String TELEPORT_HUBS_FILE = "teleport_hubs.json";
    
    // Хранилища данных
    private final Map<String, Waypoint> waypoints = new ConcurrentHashMap<>();
    private final Map<String, Road> roads = new ConcurrentHashMap<>();
    private final Map<String, TeleportHub> teleportHubs = new ConcurrentHashMap<>();
    
    // Кэш для быстрого поиска
    private final Map<String, List<Waypoint>> waypointsByType = new ConcurrentHashMap<>();
    private final Map<ChunkCoordinate, List<Waypoint>> waypointsByChunk = new ConcurrentHashMap<>();
    
    private final Level world;
    private final String craftoName;
    
    public WaypointSystem(Level world, String craftoName) {
        this.world = world;
        this.craftoName = craftoName;
        ensureDirectoryExists();
        loadAllData();
        rebuildCaches();
    }
    
    /**
     * Создает новую путевую точку
     */
    public Waypoint createWaypoint(String name, BlockPos position, WaypointType type) {
        return createWaypoint(name, position, type, null);
    }
    
    /**
     * Создает новую путевую точку с описанием
     */
    public Waypoint createWaypoint(String name, BlockPos position, WaypointType type, String description) {
        if (waypoints.containsKey(name)) {
            throw new IllegalArgumentException("Путевая точка с именем '" + name + "' уже существует");
        }
        
        Waypoint waypoint = new Waypoint(name, position, type, description);
        waypoint.setCreatedBy(craftoName);
        
        waypoints.put(name, waypoint);
        updateCaches(waypoint);
        
        // Автоматически создаем маяк для важных точек
        if (type == WaypointType.BASE || type == WaypointType.RESOURCE_SITE || type == WaypointType.TELEPORT_HUB) {
            createBeacon(waypoint);
        }
        
        saveWaypoints();
        return waypoint;
    }
    
    /**
     * Создает маяк в путевой точке
     */
    private void createBeacon(Waypoint waypoint) {
        // В реальной реализации здесь будет код для размещения маяка
        // Пока что просто отмечаем, что маяк должен быть создан
        waypoint.setHasBeacon(true);
        System.out.println("Создан маяк в точке: " + waypoint.getName() + " " + waypoint.getPosition());
    }
    
    /**
     * Удаляет путевую точку
     */
    public boolean removeWaypoint(String name) {
        Waypoint removed = waypoints.remove(name);
        if (removed != null) {
            removeCaches(removed);
            saveWaypoints();
            return true;
        }
        return false;
    }
    
    /**
     * Получает путевую точку по имени
     */
    public Optional<Waypoint> getWaypoint(String name) {
        return Optional.ofNullable(waypoints.get(name));
    }
    
    /**
     * Получает все путевые точки определенного типа
     */
    public List<Waypoint> getWaypointsByType(WaypointType type) {
        return waypointsByType.getOrDefault(type.toString(), new ArrayList<>());
    }
    
    /**
     * Находит ближайшую путевую точку к указанной позиции
     */
    public Optional<Waypoint> findNearestWaypoint(BlockPos position) {
        return findNearestWaypoint(position, null);
    }
    
    /**
     * Находит ближайшую путевую точку определенного типа
     */
    public Optional<Waypoint> findNearestWaypoint(BlockPos position, WaypointType type) {
        Collection<Waypoint> candidates = type != null ? 
            getWaypointsByType(type) : waypoints.values();
        
        return candidates.stream()
                .filter(Waypoint::isActive)
                .min(Comparator.comparing(w -> w.getDistanceFrom(position)));
    }
    
    /**
     * Находит путевые точки в радиусе
     */
    public List<Waypoint> findWaypointsInRadius(BlockPos center, double radius) {
        return waypoints.values().stream()
                .filter(Waypoint::isActive)
                .filter(w -> w.getDistanceFrom(center) <= radius)
                .sorted(Comparator.comparing(w -> w.getDistanceFrom(center)))
                .collect(Collectors.toList());
    }
    
    /**
     * Создает дорогу между двумя путевыми точками
     */
    public Road createRoad(String roadId, String startWaypointName, String endWaypointName, RoadType roadType) {
        Waypoint start = waypoints.get(startWaypointName);
        Waypoint end = waypoints.get(endWaypointName);
        
        if (start == null || end == null) {
            throw new IllegalArgumentException("Одна или обе путевые точки не найдены");
        }
        
        if (roads.containsKey(roadId)) {
            throw new IllegalArgumentException("Дорога с ID '" + roadId + "' уже существует");
        }
        
        Road road = new Road(roadId, start.getPosition(), end.getPosition(), roadType);
        road.setStartWaypoint(startWaypointName);
        road.setEndWaypoint(endWaypointName);
        road.setCreatedBy(craftoName);
        
        roads.put(roadId, road);
        
        // Обновляем связи в путевых точках
        start.addConnectedRoad(roadId);
        end.addConnectedRoad(roadId);
        
        saveRoads();
        saveWaypoints();
        
        return road;
    }
    
    /**
     * Находит кратчайший путь между двумя точками
     */
    public Optional<NavigationPath> findPath(BlockPos start, BlockPos destination) {
        // Находим ближайшие путевые точки к начальной и конечной позициям
        Optional<Waypoint> startWaypoint = findNearestWaypoint(start);
        Optional<Waypoint> endWaypoint = findNearestWaypoint(destination);
        
        if (startWaypoint.isEmpty() || endWaypoint.isEmpty()) {
            // Прямой путь, если нет подходящих путевых точек
            return Optional.of(new NavigationPath(Arrays.asList(start, destination)));
        }
        
        // Используем алгоритм Дейкстры для поиска кратчайшего пути
        List<BlockPos> path = findShortestPath(startWaypoint.get(), endWaypoint.get());
        
        if (path.isEmpty()) {
            return Optional.empty();
        }
        
        // Добавляем начальную и конечную позиции
        List<BlockPos> fullPath = new ArrayList<>();
        fullPath.add(start);
        fullPath.addAll(path);
        fullPath.add(destination);
        
        return Optional.of(new NavigationPath(fullPath));
    }
    
    /**
     * Алгоритм Дейкстры для поиска кратчайшего пути между путевыми точками
     */
    private List<BlockPos> findShortestPath(Waypoint start, Waypoint end) {
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(
            Comparator.comparing(distances::get)
        );
        
        // Инициализация
        for (String waypointName : waypoints.keySet()) {
            distances.put(waypointName, Double.MAX_VALUE);
        }
        distances.put(start.getName(), 0.0);
        queue.add(start.getName());
        
        while (!queue.isEmpty()) {
            String currentName = queue.poll();
            Waypoint current = waypoints.get(currentName);
            
            if (current.getName().equals(end.getName())) {
                break; // Достигли цели
            }
            
            // Проверяем все соединенные дороги
            for (String roadId : current.getConnectedRoads()) {
                Road road = roads.get(roadId);
                if (road == null || !road.isActive()) continue;
                
                String neighborName = road.getOtherWaypoint(currentName);
                if (neighborName == null) continue;
                
                double newDistance = distances.get(currentName) + road.getDistance();
                
                if (newDistance < distances.get(neighborName)) {
                    distances.put(neighborName, newDistance);
                    previous.put(neighborName, currentName);
                    queue.add(neighborName);
                }
            }
        }
        
        // Восстанавливаем путь
        List<BlockPos> path = new ArrayList<>();
        String current = end.getName();
        
        while (previous.containsKey(current)) {
            Waypoint waypoint = waypoints.get(current);
            path.add(0, waypoint.getPosition());
            current = previous.get(current);
        }
        
        // Добавляем начальную точку
        path.add(0, start.getPosition());
        
        return path;
    }
    
    /**
     * Создает телепортационный хаб
     */
    public TeleportHub createTeleportHub(String hubId, BlockPos position, String name) {
        if (teleportHubs.containsKey(hubId)) {
            throw new IllegalArgumentException("Телепортационный хаб с ID '" + hubId + "' уже существует");
        }
        
        TeleportHub hub = new TeleportHub(hubId, position, name);
        hub.setCreatedBy(craftoName);
        
        teleportHubs.put(hubId, hub);
        
        // Создаем соответствующую путевую точку
        createWaypoint("hub_" + hubId, position, WaypointType.TELEPORT_HUB, "Телепортационный хаб: " + name);
        
        saveTeleportHubs();
        return hub;
    }
    
    /**
     * Получает все доступные телепортационные хабы
     */
    public List<TeleportHub> getAvailableTeleportHubs() {
        return teleportHubs.values().stream()
                .filter(TeleportHub::isActive)
                .sorted(Comparator.comparing(TeleportHub::getName))
                .collect(Collectors.toList());
    }
    
    /**
     * Находит ближайший телепортационный хаб
     */
    public Optional<TeleportHub> findNearestTeleportHub(BlockPos position) {
        return teleportHubs.values().stream()
                .filter(TeleportHub::isActive)
                .min(Comparator.comparing(hub -> hub.getDistanceFrom(position)));
    }
    
    /**
     * Получает статистику навигационной системы
     */
    public NavigationStats getNavigationStats() {
        int totalWaypoints = waypoints.size();
        int activeWaypoints = (int) waypoints.values().stream().filter(Waypoint::isActive).count();
        int totalRoads = roads.size();
        int activeRoads = (int) roads.values().stream().filter(Road::isActive).count();
        int totalHubs = teleportHubs.size();
        
        Map<String, Integer> waypointTypeCount = new HashMap<>();
        for (WaypointType type : WaypointType.values()) {
            waypointTypeCount.put(type.toString(), getWaypointsByType(type).size());
        }
        
        double totalRoadDistance = roads.values().stream()
                .filter(Road::isActive)
                .mapToDouble(Road::getDistance)
                .sum();
        
        return new NavigationStats(totalWaypoints, activeWaypoints, totalRoads, 
                                 activeRoads, totalHubs, waypointTypeCount, totalRoadDistance);
    }
    
    // Методы для работы с кэшем
    
    private void rebuildCaches() {
        waypointsByType.clear();
        waypointsByChunk.clear();
        
        for (Waypoint waypoint : waypoints.values()) {
            updateCaches(waypoint);
        }
    }
    
    private void updateCaches(Waypoint waypoint) {
        // Кэш по типу
        String type = waypoint.getType().toString();
        waypointsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(waypoint);
        
        // Кэш по чанку
        ChunkCoordinate chunk = new ChunkCoordinate(
            waypoint.getPosition().getX() >> 4,
            waypoint.getPosition().getZ() >> 4
        );
        waypointsByChunk.computeIfAbsent(chunk, k -> new ArrayList<>()).add(waypoint);
    }
    
    private void removeCaches(Waypoint waypoint) {
        // Удаляем из кэша по типу
        String type = waypoint.getType().toString();
        List<Waypoint> typeList = waypointsByType.get(type);
        if (typeList != null) {
            typeList.remove(waypoint);
        }
        
        // Удаляем из кэша по чанку
        ChunkCoordinate chunk = new ChunkCoordinate(
            waypoint.getPosition().getX() >> 4,
            waypoint.getPosition().getZ() >> 4
        );
        List<Waypoint> chunkList = waypointsByChunk.get(chunk);
        if (chunkList != null) {
            chunkList.remove(waypoint);
        }
    }
    
    // Методы сохранения и загрузки данных
    
    private void ensureDirectoryExists() {
        File dir = new File(WAYPOINT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    private void loadAllData() {
        loadWaypoints();
        loadRoads();
        loadTeleportHubs();
    }
    
    private void saveAllData() {
        saveWaypoints();
        saveRoads();
        saveTeleportHubs();
    }
    
    private void loadWaypoints() {
        try {
            File file = new File(WAYPOINT_DIR + WAYPOINTS_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, Waypoint>>(){}.getType();
                Map<String, Waypoint> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    waypoints.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки путевых точек: " + e.getMessage());
        }
    }
    
    private void saveWaypoints() {
        try (FileWriter writer = new FileWriter(WAYPOINT_DIR + WAYPOINTS_FILE)) {
            GSON.toJson(waypoints, writer);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения путевых точек: " + e.getMessage());
        }
    }
    
    private void loadRoads() {
        try {
            File file = new File(WAYPOINT_DIR + ROADS_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, Road>>(){}.getType();
                Map<String, Road> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    roads.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки дорог: " + e.getMessage());
        }
    }
    
    private void saveRoads() {
        try (FileWriter writer = new FileWriter(WAYPOINT_DIR + ROADS_FILE)) {
            GSON.toJson(roads, writer);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения дорог: " + e.getMessage());
        }
    }
    
    private void loadTeleportHubs() {
        try {
            File file = new File(WAYPOINT_DIR + TELEPORT_HUBS_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, TeleportHub>>(){}.getType();
                Map<String, TeleportHub> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    teleportHubs.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки телепортационных хабов: " + e.getMessage());
        }
    }
    
    private void saveTeleportHubs() {
        try (FileWriter writer = new FileWriter(WAYPOINT_DIR + TELEPORT_HUBS_FILE)) {
            GSON.toJson(teleportHubs, writer);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения телепортационных хабов: " + e.getMessage());
        }
    }
}