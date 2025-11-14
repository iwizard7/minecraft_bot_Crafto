package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Система исследования для умной разведки территории
 * Включает картографирование, поиск ресурсов, оценку опасностей и создание торговых маршрутов
 */
public class ExplorationSystem {
    
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();
    
    private static final String EXPLORATION_DIR = "config/crafto/exploration/";
    private static final String EXPLORED_AREAS_FILE = "explored_areas.json";
    private static final String RESOURCE_LOCATIONS_FILE = "resource_locations.json";
    private static final String DANGER_ZONES_FILE = "danger_zones.json";
    private static final String TRADE_ROUTES_FILE = "trade_routes.json";
    
    // Радиус исследования за один раз
    private static final int EXPLORATION_RADIUS = 64;
    private static final int MAX_EXPLORATION_DISTANCE = 1000;
    private static final int DANGER_ASSESSMENT_RADIUS = 32;
    
    // Хранилища данных
    private final Map<ChunkCoordinate, ExploredArea> exploredAreas = new ConcurrentHashMap<>();
    private final Map<String, List<ResourceLocation>> resourceLocations = new ConcurrentHashMap<>();
    private final Map<ChunkCoordinate, DangerZone> dangerZones = new ConcurrentHashMap<>();
    private final List<TradeRoute> tradeRoutes = new ArrayList<>();
    
    // Очередь исследования
    private final Queue<ExplorationTask> explorationQueue = new LinkedList<>();
    private final Set<ChunkCoordinate> scheduledForExploration = new HashSet<>();
    
    private final Level world;
    private final String craftoName;
    
    public ExplorationSystem(Level world, String craftoName) {
        this.world = world;
        this.craftoName = craftoName;
        ensureDirectoryExists();
        loadAllData();
    }
    
    /**
     * Начинает исследование области вокруг указанной позиции
     */
    public CompletableFuture<ExplorationResult> exploreArea(BlockPos centerPos, int radius) {
        return CompletableFuture.supplyAsync(() -> {
            ExplorationResult result = new ExplorationResult();
            
            try {
                // Определяем границы исследования
                int minX = centerPos.getX() - radius;
                int maxX = centerPos.getX() + radius;
                int minZ = centerPos.getZ() - radius;
                int maxZ = centerPos.getZ() + radius;
                
                // Исследуем по чанкам
                for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
                    for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                        ChunkCoordinate coord = new ChunkCoordinate(chunkX, chunkZ);
                        
                        if (!exploredAreas.containsKey(coord)) {
                            ExploredArea area = exploreChunk(coord);
                            exploredAreas.put(coord, area);
                            result.addExploredArea(area);
                        }
                    }
                }
                
                // Анализируем найденные ресурсы
                analyzeResources(result);
                
                // Оцениваем опасности
                assessDangers(result);
                
                // Сохраняем результаты
                saveAllData();
                
                result.setSuccess(true);
                
            } catch (Exception e) {
                result.setSuccess(false);
                result.setErrorMessage("Ошибка при исследовании: " + e.getMessage());
                System.err.println("Ошибка в ExplorationSystem.exploreArea: " + e.getMessage());
            }
            
            return result;
        });
    }
    
    /**
     * Исследует конкретный чанк
     */
    private ExploredArea exploreChunk(ChunkCoordinate coord) {
        ExploredArea area = new ExploredArea(coord);
        
        int startX = coord.x << 4;
        int startZ = coord.z << 4;
        
        // Сканируем блоки в чанке
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                exploreColumn(x, z, area);
            }
        }
        
        // Определяем биом
        BlockPos samplePos = new BlockPos(startX + 8, 64, startZ + 8);
        Biome biome = world.getBiome(samplePos).value();
        area.setBiome(biome.toString());
        
        // Анализируем структуры
        analyzeStructures(area);
        
        area.setExplorationTime(LocalDateTime.now());
        
        return area;
    }
    
    /**
     * Исследует колонку блоков от коренной породы до неба
     */
    private void exploreColumn(int x, int z, ExploredArea area) {
        Map<String, Integer> blockCounts = new HashMap<>();
        List<ResourceLocation> resources = new ArrayList<>();
        
        // Сканируем от коренной породы до поверхности
        for (int y = world.getMinBuildHeight(); y <= world.getMaxBuildHeight(); y++) {
            BlockPos pos = new BlockPos(x, y, z);
            Block block = world.getBlockState(pos).getBlock();
            
            if (block == Blocks.AIR || block == Blocks.CAVE_AIR) {
                continue;
            }
            
            String blockName = block.toString();
            blockCounts.merge(blockName, 1, Integer::sum);
            
            // Проверяем, является ли блок ресурсом
            if (isValuableResource(block)) {
                ResourceLocation resource = new ResourceLocation(
                    pos, blockName, calculateResourceValue(block), LocalDateTime.now()
                );
                resources.add(resource);
            }
        }
        
        area.addBlockCounts(blockCounts);
        area.addResources(resources);
    }
    
    /**
     * Проверяет, является ли блок ценным ресурсом
     */
    private boolean isValuableResource(Block block) {
        return block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE ||
               block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE ||
               block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE ||
               block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE ||
               block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE ||
               block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE ||
               block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE ||
               block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE ||
               block == Blocks.ANCIENT_DEBRIS ||
               block == Blocks.OBSIDIAN ||
               block == Blocks.SPAWNER;
    }
    
    /**
     * Вычисляет ценность ресурса
     */
    private int calculateResourceValue(Block block) {
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return 100;
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return 90;
        if (block == Blocks.ANCIENT_DEBRIS) return 150;
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) return 60;
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return 40;
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return 20;
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return 10;
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return 30;
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return 25;
        if (block == Blocks.OBSIDIAN) return 15;
        if (block == Blocks.SPAWNER) return 200;
        return 1;
    }
    
    /**
     * Анализирует структуры в области
     */
    private void analyzeStructures(ExploredArea area) {
        // Здесь можно добавить логику поиска деревень, храмов, крепостей и т.д.
        // Пока что базовая реализация
        ChunkCoordinate coord = area.getCoordinate();
        int centerX = (coord.x << 4) + 8;
        int centerZ = (coord.z << 4) + 8;
        
        // Простая эвристика для обнаружения структур
        // В реальной реализации нужно использовать StructureManager
        if (ThreadLocalRandom.current().nextDouble() < 0.05) { // 5% шанс найти структуру
            area.addStructure("unknown_structure", new BlockPos(centerX, 64, centerZ));
        }
    }
    
    /**
     * Анализирует найденные ресурсы
     */
    private void analyzeResources(ExplorationResult result) {
        for (ExploredArea area : result.getExploredAreas()) {
            for (ResourceLocation resource : area.getResources()) {
                String resourceType = resource.getResourceType();
                resourceLocations.computeIfAbsent(resourceType, k -> new ArrayList<>()).add(resource);
            }
        }
    }
    
    /**
     * Оценивает опасности в исследованной области
     */
    private void assessDangers(ExplorationResult result) {
        for (ExploredArea area : result.getExploredAreas()) {
            DangerLevel dangerLevel = calculateDangerLevel(area);
            
            if (dangerLevel.getLevel() > 3) { // Высокий уровень опасности
                DangerZone dangerZone = new DangerZone(
                    area.getCoordinate(),
                    dangerLevel,
                    "Высокий уровень опасности обнаружен",
                    LocalDateTime.now()
                );
                dangerZones.put(area.getCoordinate(), dangerZone);
            }
        }
    }
    
    /**
     * Вычисляет уровень опасности для области
     */
    private DangerLevel calculateDangerLevel(ExploredArea area) {
        int dangerScore = 0;
        String primaryThreat = "Неизвестно";
        
        // Проверяем наличие спавнеров
        long spawnerCount = area.getResources().stream()
                .filter(r -> r.getResourceType().contains("spawner"))
                .count();
        if (spawnerCount > 0) {
            dangerScore += spawnerCount * 50;
            primaryThreat = "Спавнеры мобов";
        }
        
        // Проверяем биом
        String biome = area.getBiome();
        if (biome.contains("nether") || biome.contains("end")) {
            dangerScore += 100;
            primaryThreat = "Опасный биом";
        } else if (biome.contains("desert") || biome.contains("jungle")) {
            dangerScore += 20;
        }
        
        // Проверяем наличие лавы
        Map<String, Integer> blockCounts = area.getBlockCounts();
        int lavaCount = blockCounts.getOrDefault("minecraft:lava", 0);
        if (lavaCount > 10) {
            dangerScore += lavaCount * 2;
            primaryThreat = "Лава";
        }
        
        // Определяем уровень опасности (1-10)
        int level = Math.min(10, Math.max(1, dangerScore / 20));
        
        return new DangerLevel(level, primaryThreat, dangerScore);
    }
    
    /**
     * Планирует исследование неизвестных областей
     */
    public void scheduleExploration(BlockPos currentPos, int maxDistance) {
        // Находим ближайшие неисследованные области
        List<ChunkCoordinate> unexplored = findUnexploredAreas(currentPos, maxDistance);
        
        // Сортируем по приоритету (расстояние + потенциальная ценность)
        unexplored.sort((a, b) -> {
            double distA = calculateDistance(currentPos, a);
            double distB = calculateDistance(currentPos, b);
            return Double.compare(distA, distB);
        });
        
        // Добавляем в очередь исследования
        for (ChunkCoordinate coord : unexplored) {
            if (!scheduledForExploration.contains(coord)) {
                ExplorationTask task = new ExplorationTask(coord, calculatePriority(currentPos, coord));
                explorationQueue.offer(task);
                scheduledForExploration.add(coord);
            }
        }
    }
    
    /**
     * Находит неисследованные области в радиусе
     */
    private List<ChunkCoordinate> findUnexploredAreas(BlockPos center, int maxDistance) {
        List<ChunkCoordinate> unexplored = new ArrayList<>();
        
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        int chunkRadius = maxDistance >> 4;
        
        for (int x = centerChunkX - chunkRadius; x <= centerChunkX + chunkRadius; x++) {
            for (int z = centerChunkZ - chunkRadius; z <= centerChunkZ + chunkRadius; z++) {
                ChunkCoordinate coord = new ChunkCoordinate(x, z);
                if (!exploredAreas.containsKey(coord)) {
                    unexplored.add(coord);
                }
            }
        }
        
        return unexplored;
    }
    
    /**
     * Вычисляет расстояние от позиции до чанка
     */
    private double calculateDistance(BlockPos pos, ChunkCoordinate chunk) {
        int chunkCenterX = (chunk.x << 4) + 8;
        int chunkCenterZ = (chunk.z << 4) + 8;
        
        double dx = pos.getX() - chunkCenterX;
        double dz = pos.getZ() - chunkCenterZ;
        
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Вычисляет приоритет исследования области
     */
    private int calculatePriority(BlockPos currentPos, ChunkCoordinate coord) {
        double distance = calculateDistance(currentPos, coord);
        
        // Чем ближе, тем выше приоритет
        int priority = (int) (1000 - distance);
        
        // Добавляем бонус за потенциально интересные области
        // (например, рядом с уже найденными ресурсами)
        for (ChunkCoordinate explored : exploredAreas.keySet()) {
            if (Math.abs(explored.x - coord.x) <= 1 && Math.abs(explored.z - coord.z) <= 1) {
                ExploredArea area = exploredAreas.get(explored);
                if (!area.getResources().isEmpty()) {
                    priority += 100; // Бонус за близость к ресурсам
                }
            }
        }
        
        return Math.max(1, priority);
    }
    
    /**
     * Получает следующую задачу исследования
     */
    public Optional<ExplorationTask> getNextExplorationTask() {
        ExplorationTask task = explorationQueue.poll();
        if (task != null) {
            scheduledForExploration.remove(task.getCoordinate());
            return Optional.of(task);
        }
        return Optional.empty();
    }
    
    /**
     * Получает информацию об исследованной области
     */
    public Optional<ExploredArea> getExploredArea(ChunkCoordinate coord) {
        return Optional.ofNullable(exploredAreas.get(coord));
    }
    
    /**
     * Получает все найденные ресурсы определенного типа
     */
    public List<ResourceLocation> getResourceLocations(String resourceType) {
        return resourceLocations.getOrDefault(resourceType, new ArrayList<>());
    }
    
    /**
     * Получает все опасные зоны
     */
    public Collection<DangerZone> getDangerZones() {
        return dangerZones.values();
    }
    
    /**
     * Проверяет, является ли область опасной
     */
    public boolean isDangerous(BlockPos pos) {
        ChunkCoordinate coord = new ChunkCoordinate(pos.getX() >> 4, pos.getZ() >> 4);
        DangerZone danger = dangerZones.get(coord);
        return danger != null && danger.getDangerLevel().getLevel() > 5;
    }
    
    /**
     * Получает статистику исследования
     */
    public ExplorationStats getExplorationStats() {
        int totalExplored = exploredAreas.size();
        int totalResources = resourceLocations.values().stream().mapToInt(List::size).sum();
        int dangerousAreas = dangerZones.size();
        
        Map<String, Integer> resourceCounts = new HashMap<>();
        for (Map.Entry<String, List<ResourceLocation>> entry : resourceLocations.entrySet()) {
            resourceCounts.put(entry.getKey(), entry.getValue().size());
        }
        
        return new ExplorationStats(totalExplored, totalResources, dangerousAreas, resourceCounts);
    }
    
    // Методы сохранения и загрузки данных
    
    private void ensureDirectoryExists() {
        File dir = new File(EXPLORATION_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    private void loadAllData() {
        loadExploredAreas();
        loadResourceLocations();
        loadDangerZones();
        loadTradeRoutes();
    }
    
    private void saveAllData() {
        saveExploredAreas();
        saveResourceLocations();
        saveDangerZones();
        saveTradeRoutes();
    }
    
    private void loadExploredAreas() {
        try {
            File file = new File(EXPLORATION_DIR + EXPLORED_AREAS_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<ChunkCoordinate, ExploredArea>>(){}.getType();
                Map<ChunkCoordinate, ExploredArea> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    exploredAreas.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки исследованных областей: " + e.getMessage());
        }
    }
    
    private void saveExploredAreas() {
        try (FileWriter writer = new FileWriter(EXPLORATION_DIR + EXPLORED_AREAS_FILE)) {
            GSON.toJson(exploredAreas, writer);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения исследованных областей: " + e.getMessage());
        }
    }
    
    private void loadResourceLocations() {
        try {
            File file = new File(EXPLORATION_DIR + RESOURCE_LOCATIONS_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, List<ResourceLocation>>>(){}.getType();
                Map<String, List<ResourceLocation>> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    resourceLocations.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки местоположений ресурсов: " + e.getMessage());
        }
    }
    
    private void saveResourceLocations() {
        try (FileWriter writer = new FileWriter(EXPLORATION_DIR + RESOURCE_LOCATIONS_FILE)) {
            GSON.toJson(resourceLocations, writer);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения местоположений ресурсов: " + e.getMessage());
        }
    }
    
    private void loadDangerZones() {
        try {
            File file = new File(EXPLORATION_DIR + DANGER_ZONES_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<ChunkCoordinate, DangerZone>>(){}.getType();
                Map<ChunkCoordinate, DangerZone> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    dangerZones.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки опасных зон: " + e.getMessage());
        }
    }
    
    private void saveDangerZones() {
        try (FileWriter writer = new FileWriter(EXPLORATION_DIR + DANGER_ZONES_FILE)) {
            GSON.toJson(dangerZones, writer);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения опасных зон: " + e.getMessage());
        }
    }
    
    private void loadTradeRoutes() {
        try {
            File file = new File(EXPLORATION_DIR + TRADE_ROUTES_FILE);
            if (!file.exists()) return;
            
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<List<TradeRoute>>(){}.getType();
                List<TradeRoute> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    tradeRoutes.addAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки торговых маршрутов: " + e.getMessage());
        }
    }
    
    private void saveTradeRoutes() {
        try (FileWriter writer = new FileWriter(EXPLORATION_DIR + TRADE_ROUTES_FILE)) {
            GSON.toJson(tradeRoutes, writer);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения торговых маршрутов: " + e.getMessage());
        }
    }
}