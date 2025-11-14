package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Совместная карта для экспорта и обмена между игроками
 */
public class SharedMap {
    private String mapId;
    private String name;
    private String description;
    private BlockPos center;
    private int radius;
    private LocalDateTime creationTime;
    private LocalDateTime lastUpdated;
    private String createdBy;
    private List<MapChunk> chunks;
    private List<MapMarker> markers;
    private List<Waypoint> waypoints;
    private boolean isPublic;
    private List<String> authorizedUsers;
    private String version;
    private long fileSize;
    
    // Конструктор по умолчанию для JSON десериализации
    public SharedMap() {
        this.chunks = new ArrayList<>();
        this.markers = new ArrayList<>();
        this.waypoints = new ArrayList<>();
        this.authorizedUsers = new ArrayList<>();
    }
    
    public SharedMap(String mapId, String name, BlockPos center, int radius) {
        this();
        this.mapId = mapId;
        this.name = name;
        this.center = center;
        this.radius = radius;
        this.creationTime = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.isPublic = true;
        this.version = "1.0";
    }
    
    /**
     * Обновляет карту новыми данными
     */
    public void updateMap(List<MapChunk> newChunks, List<MapMarker> newMarkers, List<Waypoint> newWaypoints) {
        if (newChunks != null) {
            this.chunks = new ArrayList<>(newChunks);
        }
        if (newMarkers != null) {
            this.markers = new ArrayList<>(newMarkers);
        }
        if (newWaypoints != null) {
            this.waypoints = new ArrayList<>(newWaypoints);
        }
        
        this.lastUpdated = LocalDateTime.now();
        updateVersion();
        calculateFileSize();
    }
    
    /**
     * Добавляет пользователя в список авторизованных
     */
    public void authorizeUser(String userName) {
        if (!authorizedUsers.contains(userName)) {
            authorizedUsers.add(userName);
        }
    }
    
    /**
     * Удаляет пользователя из списка авторизованных
     */
    public void revokeUser(String userName) {
        authorizedUsers.remove(userName);
    }
    
    /**
     * Проверяет, может ли пользователь получить доступ к карте
     */
    public boolean canAccess(String userName) {
        if (isPublic) {
            return true;
        }
        
        return userName.equals(createdBy) || authorizedUsers.contains(userName);
    }
    
    /**
     * Получает статистику карты
     */
    public MapStatistics getStatistics() {
        int totalChunks = chunks.size();
        int totalMarkers = markers.size();
        int totalWaypoints = waypoints.size();
        
        // Подсчет маркеров по типам
        long resourceMarkers = markers.stream().filter(m -> m.getType() == MarkerType.RESOURCE).count();
        long structureMarkers = markers.stream().filter(m -> m.getType() == MarkerType.STRUCTURE).count();
        long dangerMarkers = markers.stream().filter(m -> m.getType() == MarkerType.DANGER).count();
        
        // Подсчет путевых точек по типам
        long baseWaypoints = waypoints.stream().filter(w -> w.getType() == WaypointType.BASE).count();
        long resourceWaypoints = waypoints.stream().filter(w -> w.getType() == WaypointType.RESOURCE_SITE).count();
        
        // Вычисление покрытой области
        long coveredArea = (long) totalChunks * 256; // 16x16 блоков на чанк
        
        return new MapStatistics(totalChunks, totalMarkers, totalWaypoints, 
                                (int) resourceMarkers, (int) structureMarkers, (int) dangerMarkers,
                                (int) baseWaypoints, (int) resourceWaypoints, coveredArea);
    }
    
    /**
     * Получает краткое описание карты
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Карта: ").append(name).append("\n");
        summary.append("Центр: ").append(formatPosition(center)).append("\n");
        summary.append("Радиус: ").append(radius).append(" блоков\n");
        summary.append("Создана: ").append(creationTime).append("\n");
        summary.append("Обновлена: ").append(lastUpdated).append("\n");
        summary.append("Создатель: ").append(createdBy).append("\n");
        summary.append("Версия: ").append(version).append("\n");
        
        MapStatistics stats = getStatistics();
        summary.append("Чанков: ").append(stats.getTotalChunks()).append("\n");
        summary.append("Маркеров: ").append(stats.getTotalMarkers()).append("\n");
        summary.append("Путевых точек: ").append(stats.getTotalWaypoints()).append("\n");
        summary.append("Покрытая область: ").append(stats.getCoveredArea()).append(" блоков²\n");
        summary.append("Размер файла: ").append(formatFileSize()).append("\n");
        summary.append("Доступ: ").append(isPublic ? "Публичный" : "Ограниченный");
        
        return summary.toString();
    }
    
    /**
     * Обновляет версию карты
     */
    private void updateVersion() {
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            try {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                minor++;
                if (minor >= 10) {
                    major++;
                    minor = 0;
                }
                version = major + "." + minor;
            } catch (NumberFormatException e) {
                version = "1.0";
            }
        } else {
            version = "1.0";
        }
    }
    
    /**
     * Вычисляет примерный размер файла карты
     */
    private void calculateFileSize() {
        // Примерная оценка размера в байтах
        long chunkSize = chunks.size() * 500L; // ~500 байт на чанк
        long markerSize = markers.size() * 200L; // ~200 байт на маркер
        long waypointSize = waypoints.size() * 300L; // ~300 байт на путевую точку
        
        fileSize = chunkSize + markerSize + waypointSize + 1000L; // +1KB для метаданных
    }
    
    /**
     * Форматирует размер файла для отображения
     */
    private String formatFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
    
    /**
     * Форматирует позицию для отображения
     */
    private String formatPosition(BlockPos pos) {
        if (pos == null) return "неизвестно";
        return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }
    
    // Геттеры и сеттеры
    
    public String getMapId() {
        return mapId;
    }
    
    public void setMapId(String mapId) {
        this.mapId = mapId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BlockPos getCenter() {
        return center;
    }
    
    public void setCenter(BlockPos center) {
        this.center = center;
    }
    
    public int getRadius() {
        return radius;
    }
    
    public void setRadius(int radius) {
        this.radius = radius;
    }
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public List<MapChunk> getChunks() {
        return chunks;
    }
    
    public void setChunks(List<MapChunk> chunks) {
        this.chunks = chunks;
        calculateFileSize();
    }
    
    public List<MapMarker> getMarkers() {
        return markers;
    }
    
    public void setMarkers(List<MapMarker> markers) {
        this.markers = markers;
        calculateFileSize();
    }
    
    public List<Waypoint> getWaypoints() {
        return waypoints;
    }
    
    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
        calculateFileSize();
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    public List<String> getAuthorizedUsers() {
        return authorizedUsers;
    }
    
    public void setAuthorizedUsers(List<String> authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    @Override
    public String toString() {
        return String.format("SharedMap{id='%s', name='%s', chunks=%d, markers=%d, waypoints=%d}", 
                           mapId, name, chunks.size(), markers.size(), waypoints.size());
    }
}

/**
 * Статистика карты
 */
class MapStatistics {
    private int totalChunks;
    private int totalMarkers;
    private int totalWaypoints;
    private int resourceMarkers;
    private int structureMarkers;
    private int dangerMarkers;
    private int baseWaypoints;
    private int resourceWaypoints;
    private long coveredArea;
    
    public MapStatistics(int totalChunks, int totalMarkers, int totalWaypoints, 
                        int resourceMarkers, int structureMarkers, int dangerMarkers,
                        int baseWaypoints, int resourceWaypoints, long coveredArea) {
        this.totalChunks = totalChunks;
        this.totalMarkers = totalMarkers;
        this.totalWaypoints = totalWaypoints;
        this.resourceMarkers = resourceMarkers;
        this.structureMarkers = structureMarkers;
        this.dangerMarkers = dangerMarkers;
        this.baseWaypoints = baseWaypoints;
        this.resourceWaypoints = resourceWaypoints;
        this.coveredArea = coveredArea;
    }
    
    // Геттеры
    
    public int getTotalChunks() {
        return totalChunks;
    }
    
    public int getTotalMarkers() {
        return totalMarkers;
    }
    
    public int getTotalWaypoints() {
        return totalWaypoints;
    }
    
    public int getResourceMarkers() {
        return resourceMarkers;
    }
    
    public int getStructureMarkers() {
        return structureMarkers;
    }
    
    public int getDangerMarkers() {
        return dangerMarkers;
    }
    
    public int getBaseWaypoints() {
        return baseWaypoints;
    }
    
    public int getResourceWaypoints() {
        return resourceWaypoints;
    }
    
    public long getCoveredArea() {
        return coveredArea;
    }
}