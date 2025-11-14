package com.crafto.ai.exploration;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для системы исследования (без Minecraft зависимостей)
 */
public class ExplorationSystemTest {
    
    @Test
    void testChunkCoordinate() {
        ChunkCoordinate coord1 = new ChunkCoordinate(1, 2);
        ChunkCoordinate coord2 = new ChunkCoordinate(1, 2);
        ChunkCoordinate coord3 = new ChunkCoordinate(2, 3);
        
        assertEquals(coord1, coord2);
        assertNotEquals(coord1, coord3);
        assertEquals(coord1.hashCode(), coord2.hashCode());
        
        double distance = coord1.distanceTo(coord3);
        assertTrue(distance > 0);
        
        ChunkCoordinate[] neighbors = coord1.getNeighbors();
        assertEquals(8, neighbors.length);
    }
    
    @Test
    void testExploredArea() {
        ChunkCoordinate coord = new ChunkCoordinate(0, 0);
        ExploredArea area = new ExploredArea(coord);
        
        assertEquals(coord, area.getCoordinate());
        assertNotNull(area.getBlockCounts());
        assertNotNull(area.getResources());
        assertNotNull(area.getStructures());
        
        area.setBiome("minecraft:plains");
        assertEquals("minecraft:plains", area.getBiome());
        
        assertEquals(0, area.getTotalValue());
        assertEquals(0.0, area.getResourceDensity());
    }
    
    @Test
    void testExplorationTask() {
        ChunkCoordinate coord = new ChunkCoordinate(5, 10);
        ExplorationTask task1 = new ExplorationTask(coord, 100);
        ExplorationTask task2 = new ExplorationTask(coord, 50);
        
        assertEquals(coord, task1.getCoordinate());
        assertEquals(100, task1.getPriority());
        assertEquals("STANDARD", task1.getTaskType());
        assertFalse(task1.isCompleted());
        
        // Тест сортировки по приоритету
        assertTrue(task1.compareTo(task2) < 0); // task1 имеет больший приоритет
        
        task1.markCompleted("TestCrafto");
        assertTrue(task1.isCompleted());
        assertEquals("TestCrafto", task1.getAssignedTo());
    }
    
    @Test
    void testExplorationResult() {
        ExplorationResult result = new ExplorationResult();
        
        ChunkCoordinate coord = new ChunkCoordinate(0, 0);
        ExploredArea area = new ExploredArea(coord);
        area.setBiome("minecraft:plains");
        
        result.addExploredArea(area);
        result.setSuccess(true);
        result.setExploredBy("TestCrafto");
        
        assertTrue(result.wasSuccessful());
        assertEquals(1, result.getExploredAreas().size());
        
        String report = result.getReport();
        assertNotNull(report);
        assertTrue(report.contains("ОТЧЕТ ОБ ИССЛЕДОВАНИИ"));
        
        ExplorationSummary summary = result.getSummary();
        assertEquals(1, summary.getTotalAreas());
        assertEquals(0, summary.getTotalResources());
    }
    
    @Test
    void testExplorationStats() {
        java.util.Map<String, Integer> resourceCounts = new java.util.HashMap<>();
        resourceCounts.put("diamond", 5);
        resourceCounts.put("iron", 20);
        
        ExplorationStats stats = new ExplorationStats(10, 25, 2, resourceCounts);
        
        assertEquals(10, stats.getTotalExploredAreas());
        assertEquals(25, stats.getTotalResources());
        assertEquals(2, stats.getDangerousAreas());
        assertEquals(20.0, stats.getDangerPercentage());
        assertEquals(2.5, stats.getExplorationEfficiency());
        
        String report = stats.getReport();
        assertNotNull(report);
        assertTrue(report.contains("СТАТИСТИКА ИССЛЕДОВАНИЯ"));
    }
    
    @Test
    void testWaypointType() {
        assertEquals("Базы", WaypointType.BASE.getDisplayName());
        assertEquals("base", WaypointType.BASE.getDefaultIcon());
        assertEquals(100, WaypointType.BASE.getPriority());
        
        assertTrue(WaypointType.BASE.getPriority() > WaypointType.LANDMARK.getPriority());
    }
    
    @Test
    void testRoadType() {
        assertEquals("Незер магистраль", RoadType.NETHER_HIGHWAY.getDisplayName());
        assertEquals(8.0, RoadType.NETHER_HIGHWAY.getSpeedMultiplier());
        assertEquals(120, RoadType.NETHER_HIGHWAY.getPriority());
        
        assertTrue(RoadType.NETHER_HIGHWAY.getSpeedMultiplier() > RoadType.DIRT_PATH.getSpeedMultiplier());
    }
    
    @Test
    void testMarkerType() {
        assertEquals("Ресурс", MarkerType.RESOURCE.getDisplayName());
        assertEquals("resource", MarkerType.RESOURCE.getDefaultIcon());
        assertEquals("YELLOW", MarkerType.RESOURCE.getDefaultColor());
        assertEquals(60, MarkerType.RESOURCE.getDefaultPriority());
        
        assertTrue(MarkerType.BASE.getDefaultPriority() > MarkerType.CUSTOM.getDefaultPriority());
    }
    
    @Test
    void testNavigationPath() {
        java.util.List<net.minecraft.core.BlockPos> waypoints = new java.util.ArrayList<>();
        // Создаем простой путь без использования BlockPos для избежания проблем с Minecraft классами
        // Вместо этого тестируем логику создания пути
        
        NavigationPath path = new NavigationPath(waypoints);
        assertNotNull(path.getWaypoints());
        assertEquals(0.0, path.getTotalDistance());
        assertEquals(0, path.getEstimatedTravelTime());
        assertFalse(path.isOptimized());
        
        String summary = path.getSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("Путь из"));
    }
}