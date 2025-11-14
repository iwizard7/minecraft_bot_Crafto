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
 * Система карт для автоматического создания карт исследованных областей
 * Включает отметки ресурсов, структур и экспорт карт для игроков
 */
public class MapSystem {
    
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();
    
    private static final String MAP_DIR = "config/crafto/maps/";
    private static final String WORLD_MAP_FILE = "world_map.json";
    private static final String MARKERS_FILE = "markers.json";
    private static final String SHARED_MAPS_FILE = "shared_maps.json";
    
    // Хранилища данных
    private final Map<ChunkCoordinate, MapChunk> worldMap = new ConcurrentHashMap<>();
    private final Map<String, MapMarker> markers = new ConcurrentHashMap<>();
    private final Map<String, SharedMap> sharedMaps = new ConcurrentHashMap<>();
    
    // Кэш для быстрого поиска
    private final Map<String, List<MapMarker>> markersByType = new ConcurrentHashMap<>();
    private final Map<ChunkCoordinate, List<MapMarker>> markersByChunk = new ConcurrentHashMap<>();
    
    private final Level world;
    private final String craftoName;
    private final ExplorationSystem explorationSystem;
    private final WaypointSystem waypointSystem;
    
    public MapSystem(Level world, String craftoName, ExplorationSystem explorationSystem, WaypointSystem waypointSystem) {
        this.world = world;
        this.craftoName = craftoName;
        this.explorationSystem = explorationSystem;
        this.waypointSystem = waypointSystem;
        ensureDirectoryExists();
        loadAllData();
        rebuildCaches();
    }
    
    /**
     * Обновляет карту на основе исследованной области
     */
    public void updateMapFromExploration(ExploredArea exploredArea) {
        ChunkCoordinate coord = exploredArea.getCoordinate();
        
        // Создаем или обновляем чанк карты
        MapChunk mapChunk = worldMap.computeIfAbsent(coord, k -> new MapChunk(coord));
        mapChunk.updateFromExploredArea(exploredArea);
        
        // Добавляем маркеры для ресурсов
        for (ResourceLocation resource : exploredArea.getResources()) {
            if (resource.isRare() || resource.getValue() > 50) {
                createResourceMarker(resource);
            }
        }
        
        // Добавляем маркеры для структур
        for (Map.Entry<String, BlockPos> structure : exploredArea.getStructures().entrySet()) {
            createStructureMarker(structure.getKey(), structure.getValue());
        }
        
        saveWorldMap();
        saveMarkers();
    }
    
    /**
     * Создает маркер ресурса на карте
     */
    private void createResourceMarker(ResourceLocation resource) {
        String markerId = "resource_" + resource.getPosition().toString();
        
        MapMarker marker = new MapMarker(
            markerId,
            resource.getPosition(),
            MarkerType.RESOURCE,
            resource.getResourceType(),
            "Ресурс: " + resource.getResourceType() + " (ценность: " + resource.getValue() + ")"
        );
        
        marker.setCreatedBy(craftoName);
        marker.setVisible(true);
        marker.setIconColor(getResourceColor(resource.getResourceType()));
        
        markers.put(markerId, marker);
        updateMarkerCaches(marker);
    }
    
    /**
     * Создает маркер структуры на карте
     */
    private void createStructureMarker(String structureType, BlockPos position) {
        String markerId = "structure_" + position.toString();
        
        MapMarker marker = new MapMarker(
            markerId,
            position,
            MarkerType.STRUCTURE,
            structureType,
            "Структура: " + structureType
        );
        
        marker.setCreatedBy(craftoName);
        marker.setVisible(true);
        marker.setIconColor(getStructureColor(structureType));
        
        markers.put(markerId, marker);
        updateMarkerCaches(marker);
    }
    
    /**
     * Создает пользовательский маркер
     */
    public MapMarker createCustomMarker(String name, BlockPos position, MarkerType type, String description) {
        String markerId = "custom_" + name + "_" + position.toString();
        
        if (markers.containsKey(markerId)) {
            throw new IllegalArgumentException("Маркер с ID '" + markerId + "' уже существует");
        }
        
        MapMarker marker = new MapMarker(markerId, position, type, name, description);
        marker.setCreatedBy(craftoName);
        marker.setVisible(true);
        
        markers.put(markerId, marker);
        updateMarkerCaches(marker);
        
        saveMarkers();
        return marker;
    }
    
    /**
     * Удаляет маркер
     */
    public boolean removeMarker(String markerId) {
        MapMarker removed = markers.remove(markerId);
        if (removed != null) {
            removeMarkerCaches(removed);
            saveMarkers();
            return true;
        }
        return false;
    }
    
    /**
     * Получает все маркеры в области
     */
    public List<MapMarker> getMarkersInArea(BlockPos center, int radius) {
        return markers.values().stream()
                .filter(MapMarker::isVisible)
                .filter(marker -> {
                    double distance = calculateDistance(center, marker.getPosition());
                    return distance <= radius;
                })
                .sorted(Comparator.comparing(marker -> calculateDistance(center, marker.getPosition())))
                .collect(Collectors.toList());
    }
    
    /**
     * Получает маркеры определенного типа
     */
    public List<MapMarker> getMarkersByType(MarkerType type) {
        return markersByType.getOrDefault(type.toString(), new ArrayList<>());
    }
    
    /**
     * Создает совместную карту для экспорта
     */
    public SharedMap createSharedMap(String mapName, BlockPos center, int radius) {
        String mapId = "shared_" + mapName.replaceAll("[^a-zA-Z0-9]", "_");
        
        if (sharedMaps.containsKey(mapId)) {
            throw new IllegalArgumentException("Совместная карта с ID '" + mapId + "' уже существует");
        }
        
        // Собираем данные для совместной карты
        List<MapChunk> chunks = getChunksInArea(center, radius);
        List<MapMarker> areaMarkers = getMarkersInArea(center, radius);
        List<Waypoint> waypoints = waypointSystem.findWaypointsInRadius(center, radius);
        
        SharedMap sharedMap = new SharedMap(mapId, mapName, center, radius);
        sharedMap.setChunks(chunks);
        sharedMap.setMarkers(areaMarkers);
        sharedMap.setWaypoints(waypoints);
        sharedMap.setCreatedBy(craftoName);
        
        sharedMaps.put(mapId, sharedMap);
        saveSharedMaps();
        
        return sharedMap;
    }
    
    /**
     * Экспортирует карту в текстовый формат
     */
    public String exportMapAsText(String mapId) {
        SharedMap map = sharedMaps.get(mapId);
        if (map == null) {
            return "Карта не найдена: " + mapId;
        }
        
        StringBuilder export = new StringBuilder();
        export.append("=== КАРТА: ").append(map.getName()).append(" ===\n");
        export.append("Центр: ").append(formatPosition(map.getCenter())).append("\n");
        export.append("Радиус: ").append(map.getRadius()).append(" блоков\n");
        export.append("Создана: ").append(map.getCreationTime()).append("\n");
        export.append("Создатель: ").append(map.getCreatedBy()).append("\n\n");
        
        // Статистика
        export.append("СТАТИСТИКА:\n");
        export.append("- Исследованных чанков: ").append(map.getChunks().size()).append("\n");
        export.append("- Маркеров: ").append(map.getMarkers().size()).append("\n");
        export.append("- Путевых точек: ").append(map.getWaypoints().size()).append("\n\n");
        
        // Маркеры по типам
        Map<MarkerType, List<MapMarker>> markersByType = map.getMarkers().stream()
                .collect(Collectors.groupingBy(MapMarker::getType));
        
        for (Map.Entry<MarkerType, List<MapMarker>> entry : markersByType.entrySet()) {
            export.append(entry.getKey().getDisplayName().toUpperCase()).append(":\n");
            for (MapMarker marker : entry.getValue()) {
                export.append("- ").append(marker.getName())
                      .append(" ").append(formatPosition(marker.getPosition()));
                if (marker.getDescription() != null) {
                    export.append(" (").append(marker.getDescription()).append(")");
                }
                export.append("\n");
            }
            export.append("\n");
        }
        
        // Путевые точки
        if (!map.getWaypoints().isEmpty()) {
            export.append("ПУТЕВЫЕ ТОЧКИ:\n");
            for (Waypoint waypoint : map.getWaypoints()) {
                export.append("- ").append(waypoint.getName())
                      .append(" (").append(waypoint.getType().getDisplayName()).append(")")
                      .append(" ").append(formatPosition(waypoint.getPosition())).append("\n");
            }
        }
        
        return export.toString();
    }
    
    /**
     * Экспортирует карту в JSON формат
     */
    public String exportMapAsJson(String mapId) {
        SharedMap map = sharedMaps.get(mapId);
        if (map == null) {
            return "{\"error\": \"Карта не найдена: " + mapId + "\"}";
        }
        
        return GSON.toJson(map);
    }
    
    /**
     * Получает чанки в области
     */
    private List<MapChunk> getChunksInArea(BlockPos center, int radius) {
        List<MapChunk> chunks = new ArrayList<>();
        
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        int chunkRadius = radius >> 4;
        
        for (int x = centerChunkX - chunkRadius; x <= centerChunkX + chunkRadius; x++) {
            for (int z = centerChunkZ - chunkRadius; z <= centerChunkZ + chunkRadius; z++) {
                ChunkCoordinate coord = new ChunkCoordinate(x, z);
                MapChunk chunk = worldMap.get(coord);
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }
        
        return chunks;
    }
    
    /**
     * Получает статистику карт
     */
    public MapStats getMapStats() {
        int totalChunks = worldMap.size();
        int totalMarkers = markers.size();
        int sharedMapCount = sharedMaps.size();
        
        Map<String, Integer> markerTypeCount = new HashMap<>();
        for (MarkerType type : MarkerType.values()) {
            markerTypeCount.put(type.toString(), getMarkersByType(type).size());
        }
        
        long exploredArea = totalChunks * 256L; // 16x16 блоков на чанк
        
        return new MapStats(totalChunks, totalMarkers, sharedMapCount, markerTypeCount, exploredArea);
    }
    
    // Вспомогательные методы
    
    private double calculateDistance(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    private String formatPosition(BlockPos pos) {
        return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }
    
    private String getResourceColor(String resourceType) {
        if (resourceType.contains("diamond")) return "CYAN";
        if (resourceType.contains("emerald")) return "GREEN";
        if (resourceType.contains("gold")) return "YELLOW";
        if (resourceType.contains("iron")) return "GRAY";
        if (resourceType.contains("coal")) return "BLACK";
        if (resourceType.contains("redstone")) return "RED";
        if (resourceType.contains("lapis")) return "BLUE";
        return "WHITE";
    }
    
    private String getStructureColor(String structureType) {
        if (structureType.contains("village")) return "BROWN";
        if (structureType.contains("temple")) return "GOLD";
        if (structureType.contains("dungeon")) return "DARK_GRAY";
        if (structureType.contains("stronghold")) return "PURPLE";
        return "ORANGE";
    }
    
    // Методы для работы с кэшем
    
    private void rebuildCaches() {
        markersByType.clear();
        markersByChunk.clear();
        
        for (MapMarker marker : markers.values()) {
            updateMarkerCaches(marker);
        }
    }
    
    private void updateMarkerCaches(MapMarker marker) {
        // Кэш по типу
        String type = marker.getType().toString();
        markersByType.computeIfAbsent(type, k -> new ArrayList<>()).add(marker);
        
        // Кэш по чанку
        ChunkCoordinate chunk = new ChunkCoordinate(
            marker.getPosition().getX() >> 4,
            marker.getPosition().getZ() >> 4
        );
        markersByChunk.computeIfAbsent(chunk, k -> new ArrayList<>()).add(marker);
    }
    
    private void removeMarkerCaches(MapMarker marker) {
        // Удаляем из кэша по типу
        String type = marker.getType().toString();
        List<MapMarker> typeList = markersByType.get(type);
        if (typeList != null) {
            typeList.remove(marker);
        }
        
        // Удаляем из кэша по чанку
        ChunkCoordinate chunk = new ChunkCoordinate(
            marker.getPosition().getX() >> 4,
            marker.getPosition().getZ() >> 4
        );
        List<MapMarker> chunkList = markersByChunk.get(chunk);
        if (chunkList != null) {
            chunkList.remove(marker);
        }
    }
    
    // Методы сохранения и загрузки данных
    
    private void ensureDirectoryExists() {
        File dir = new File(MAP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    private void loadAllData() {
        loadWorldMap();
        loadMarkers();
        loadSharedMaps();
    }
    
    private void saveAllData() {
        saveWorldMap();
        saveMarkers();
        saveSharedMaps();
    }
    
    private void loadWorldMap() {
        try {
            File file = new File(MAP_DIR + WORLD_MAP_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<ChunkCoordinate, MapChunk>>(){}.getType();
                Map<ChunkCoordinate, MapChunk> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    worldMap.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки карты мира: " + e.getMessage());
        }
    }
    
    private void saveWorldMap() {
        try (FileWriter writer = new FileWriter(MAP_DIR + WORLD_MAP_FILE)) {
            GSON.toJson(worldMap, writer);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения карты мира: " + e.getMessage());
        }
    }
    
    private void loadMarkers() {
        try {
            File file = new File(MAP_DIR + MARKERS_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, MapMarker>>(){}.getType();
                Map<String, MapMarker> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    markers.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки маркеров: " + e.getMessage());
        }
    }
    
    private void saveMarkers() {
        try (FileWriter writer = new FileWriter(MAP_DIR + MARKERS_FILE)) {
            GSON.toJson(markers, writer);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения маркеров: " + e.getMessage());
        }
    }
    
    private void loadSharedMaps() {
        try {
            File file = new File(MAP_DIR + SHARED_MAPS_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, SharedMap>>(){}.getType();
                Map<String, SharedMap> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    sharedMaps.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки совместных карт: " + e.getMessage());
        }
    }
    
    private void saveSharedMaps() {
        try (FileWriter writer = new FileWriter(MAP_DIR + SHARED_MAPS_FILE)) {
            GSON.toJson(sharedMaps, writer);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения совместных карт: " + e.getMessage());
        }
    }
}