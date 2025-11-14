package com.crafto.ai.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Advanced structure generators with detailed interiors, rooms, and stairs
 */
public class AdvancedStructureGenerators {
    
    public static class BlockPlacement {
        public final BlockPos pos;
        public final Block block;
        public final BlockState state;
        
        public BlockPlacement(BlockPos pos, Block block) {
            this.pos = pos;
            this.block = block;
            this.state = block.defaultBlockState();
        }
        
        public BlockPlacement(BlockPos pos, Block block, BlockState state) {
            this.pos = pos;
            this.block = block;
            this.state = state;
        }
    }
    
    public static class Room {
        public final BlockPos start;
        public final int width, height, depth;
        public final String type; // "bedroom", "kitchen", "living", "bathroom", "storage"
        
        public Room(BlockPos start, int width, int height, int depth, String type) {
            this.start = start;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.type = type;
        }
        
        public BlockPos getCenter() {
            return start.offset(width/2, height/2, depth/2);
        }
    }
    
    public static List<BlockPlacement> generate(String structureType, BlockPos start, int width, int height, int depth, List<Block> materials) {
        // Сначала пытаемся использовать улучшенные генераторы
        try {
            // ImprovedStructureGenerators удален для упрощения
            List<BlockPlacement> improvedBlocks = null;
            if (improvedBlocks != null && !improvedBlocks.isEmpty()) {
                List<BlockPlacement> blocks = new ArrayList<>();
                for (var improvedBlock : improvedBlocks) {
                    blocks.add(new BlockPlacement(improvedBlock.pos, improvedBlock.block, improvedBlock.state));
                }
                return blocks;
            }
        } catch (Exception e) {
            // Fallback to original generators
        }
        
        // Fallback к оригинальным генераторам
        return switch (structureType.toLowerCase()) {
            case "house", "home" -> buildDetailedHouse(start, width, height, depth, materials);
            case "mansion" -> buildMansion(start, width, height, depth, materials);
            case "apartment" -> buildApartmentBuilding(start, width, height, depth, materials);
            case "castle", "fort" -> buildDetailedCastle(start, width, height, depth, materials);
            case "tower" -> buildDetailedTower(start, width, height, materials);
            case "villa" -> buildVilla(start, width, height, depth, materials);
            case "cottage" -> buildCottage(start, width, height, depth, materials);
            case "modern", "modern_house" -> buildModernHouse(start, width, height, depth, materials);
            case "skyscraper" -> buildSkyscraper(start, width, height, depth, materials);
            default -> buildDetailedHouse(start, Math.max(8, width), Math.max(6, height), Math.max(8, depth), materials);
        };
    }
    
    // Детальный дом с комнатами и лестницами
    private static List<BlockPlacement> buildDetailedHouse(BlockPos start, int width, int height, int depth, List<Block> materials) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block wallMaterial = getMaterial(materials, 0, Blocks.OAK_PLANKS);
        Block floorMaterial = getMaterial(materials, 1, Blocks.OAK_PLANKS);
        Block roofMaterial = getMaterial(materials, 2, Blocks.DARK_OAK_PLANKS);
        
        // Создаем план комнат
        List<Room> rooms = planRooms(start, width, height, depth);
        
        // Строим фундамент
        buildFoundation(blocks, start, width, depth, Blocks.COBBLESTONE);
        
        // Строим стены и комнаты
        buildWallsWithRooms(blocks, start, width, height, depth, wallMaterial, rooms);
        
        // Добавляем лестницы если дом многоэтажный
        if (height > 4) {
            addStaircase(blocks, start, width, height, depth);
        }
        
        // Добавляем мебель в комнаты
        furnishRooms(blocks, rooms);
        
        // Строим крышу
        buildDetailedRoof(blocks, start, width, height, depth, roofMaterial);
        
        // Добавляем окна
        addWindows(blocks, start, width, height, depth);
        
        // Добавляем входную дверь
        addMainEntrance(blocks, start, width, height, depth);
        
        // Добавляем двери между комнатами (после всех стен)
        addInteriorDoors(blocks, rooms, start, width, depth);
        
        return blocks;
    }
    
    // Планирование комнат
    private static List<Room> planRooms(BlockPos start, int width, int height, int depth) {
        List<Room> rooms = new ArrayList<>();
        
        // Первый этаж - простая планировка без пересечений
        if (width >= 8 && depth >= 8) {
            // Гостиная (левая половина)
            rooms.add(new Room(start.offset(1, 1, 1), width/2 - 2, 3, depth - 2, "living"));
            
            // Кухня (правая половина)
            rooms.add(new Room(start.offset(width/2 + 1, 1, 1), width/2 - 2, 3, depth/2 - 2, "kitchen"));
            
            // Ванная (правая задняя часть)
            rooms.add(new Room(start.offset(width/2 + 1, 1, depth/2 + 1), width/2 - 2, 3, depth/2 - 2, "bathroom"));
        }
        
        // Второй этаж (если есть)
        if (height > 4) {
            // Спальня 1 (левая половина)
            rooms.add(new Room(start.offset(1, 4, 1), width/2 - 2, 3, depth - 2, "bedroom"));
            
            // Спальня 2 (правая половина)
            rooms.add(new Room(start.offset(width/2 + 1, 4, 1), width/2 - 2, 3, depth - 2, "bedroom"));
        }
        
        return rooms;
    }
    
    // Строительство фундамента
    private static void buildFoundation(List<BlockPlacement> blocks, BlockPos start, int width, int depth, Block material) {
        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, -1, z), material));
            }
        }
    }
    
    // Строительство стен с учетом комнат
    private static void buildWallsWithRooms(List<BlockPlacement> blocks, BlockPos start, int width, int height, int depth, Block wallMaterial, List<Room> rooms) {
        // Внешние стены
        for (int y = 0; y < height; y++) {
            // Передняя и задняя стены
            for (int x = 0; x < width; x++) {
                blocks.add(new BlockPlacement(start.offset(x, y, 0), wallMaterial));
                blocks.add(new BlockPlacement(start.offset(x, y, depth - 1), wallMaterial));
            }
            
            // Боковые стены
            for (int z = 1; z < depth - 1; z++) {
                blocks.add(new BlockPlacement(start.offset(0, y, z), wallMaterial));
                blocks.add(new BlockPlacement(start.offset(width - 1, y, z), wallMaterial));
            }
        }
        
        // Внутренние стены между комнатами
        for (Room room : rooms) {
            buildRoomWalls(blocks, room, wallMaterial);
        }
        
        // Полы для каждого этажа
        for (int floor = 0; floor < height / 4; floor++) {
            int floorY = floor * 4;
            for (int x = 1; x < width - 1; x++) {
                for (int z = 1; z < depth - 1; z++) {
                    blocks.add(new BlockPlacement(start.offset(x, floorY, z), Blocks.OAK_PLANKS));
                }
            }
        }
    }
    
    // Строительство стен комнаты
    private static void buildRoomWalls(List<BlockPlacement> blocks, Room room, Block wallMaterial) {
        // Строим только внутренние разделительные стены между комнатами
        for (int y = 1; y < room.height; y++) {
            // Вертикальная разделительная стена (между левой и правой частями дома)
            int wallX = room.start.getX() + room.width;
            for (int z = room.start.getZ(); z < room.start.getZ() + room.depth; z++) {
                if (z != room.start.getZ() + room.depth / 2) { // Оставляем проход для двери
                    blocks.add(new BlockPlacement(new BlockPos(wallX, room.start.getY() + y, z), wallMaterial));
                }
            }
            
            // Горизонтальная разделительная стена (между передней и задней частями)
            if (room.type.equals("kitchen") || room.type.equals("bathroom")) {
                int wallZ = room.start.getZ() - 1;
                for (int x = room.start.getX(); x < room.start.getX() + room.width; x++) {
                    if (x != room.start.getX() + room.width / 2) { // Оставляем проход для двери
                        blocks.add(new BlockPlacement(new BlockPos(x, room.start.getY() + y, wallZ), wallMaterial));
                    }
                }
            }
        }
    }
    
    // Добавление лестницы
    private static void addStaircase(List<BlockPlacement> blocks, BlockPos start, int width, int height, int depth) {
        // Размещаем лестницу в углу
        int stairX = width - 3;
        int stairZ = depth - 3;
        
        // Строим лестницу по спирали
        for (int y = 1; y < height - 1; y++) {
            int step = y % 4;
            BlockPos stairPos = start.offset(stairX + (step % 2), y, stairZ + (step / 2));
            
            // Добавляем ступеньку
            blocks.add(new BlockPlacement(stairPos, Blocks.OAK_STAIRS));
            
            // Добавляем поддерживающий блок под ступенькой
            blocks.add(new BlockPlacement(stairPos.below(), Blocks.OAK_PLANKS));
        }
        
        // Площадка на втором этаже
        for (int x = stairX - 1; x <= stairX + 1; x++) {
            for (int z = stairZ - 1; z <= stairZ + 1; z++) {
                blocks.add(new BlockPlacement(start.offset(x, 4, z), Blocks.OAK_PLANKS));
            }
        }
    }
    
    // Добавление дверей между комнатами
    private static void addInteriorDoors(List<BlockPlacement> blocks, List<Room> rooms, BlockPos start, int width, int depth) {
        // Добавляем дверь между гостиной и кухней (вертикальная стена)
        if (rooms.size() >= 2) {
            BlockPos doorPos = start.offset(width / 2, 1, depth / 2);
            
            // Убираем блоки стены для двери
            blocks.removeIf(bp -> bp.pos.equals(doorPos) || bp.pos.equals(doorPos.above()));
            
            // Добавляем дверь
            blocks.add(new BlockPlacement(doorPos, Blocks.OAK_DOOR));
            blocks.add(new BlockPlacement(doorPos.above(), Blocks.OAK_DOOR));
        }
        
        // Добавляем дверь между кухней и ванной (горизонтальная стена)
        if (rooms.size() >= 3) {
            BlockPos doorPos2 = start.offset(3 * width / 4, 1, depth / 2);
            
            // Убираем блоки стены для двери
            blocks.removeIf(bp -> bp.pos.equals(doorPos2) || bp.pos.equals(doorPos2.above()));
            
            // Добавляем дверь
            blocks.add(new BlockPlacement(doorPos2, Blocks.OAK_DOOR));
            blocks.add(new BlockPlacement(doorPos2.above(), Blocks.OAK_DOOR));
        }
    }
    
    // Меблировка комнат
    private static void furnishRooms(List<BlockPlacement> blocks, List<Room> rooms) {
        for (Room room : rooms) {
            switch (room.type) {
                case "living" -> furnishLivingRoom(blocks, room);
                case "kitchen" -> furnishKitchen(blocks, room);
                case "bedroom" -> furnishBedroom(blocks, room);
                case "bathroom" -> furnishBathroom(blocks, room);
                case "storage" -> furnishStorage(blocks, room);
            }
        }
    }
    
    private static void furnishLivingRoom(List<BlockPlacement> blocks, Room room) {
        BlockPos center = room.getCenter();
        
        // Диван (используем ступеньки)
        blocks.add(new BlockPlacement(center.offset(-1, 0, 0), Blocks.QUARTZ_STAIRS));
        blocks.add(new BlockPlacement(center.offset(0, 0, 0), Blocks.QUARTZ_STAIRS));
        blocks.add(new BlockPlacement(center.offset(1, 0, 0), Blocks.QUARTZ_STAIRS));
        
        // Стол (используем забор и плиту)
        blocks.add(new BlockPlacement(center.offset(0, 0, 2), Blocks.OAK_FENCE));
        blocks.add(new BlockPlacement(center.offset(0, 1, 2), Blocks.OAK_SLAB));
        
        // Камин в углу
        BlockPos fireplace = room.start.offset(1, 0, room.depth - 2);
        blocks.add(new BlockPlacement(fireplace, Blocks.FURNACE));
        blocks.add(new BlockPlacement(fireplace.above(), Blocks.COBBLESTONE));
    }
    
    private static void furnishKitchen(List<BlockPlacement> blocks, Room room) {
        // Кухонные шкафы (сундуки)
        for (int x = 1; x < room.width - 1; x++) {
            blocks.add(new BlockPlacement(room.start.offset(x, 0, 1), Blocks.CHEST));
        }
        
        // Печь
        blocks.add(new BlockPlacement(room.start.offset(1, 0, room.depth - 2), Blocks.FURNACE));
        
        // Рабочая поверхность
        for (int x = 2; x < room.width - 1; x++) {
            blocks.add(new BlockPlacement(room.start.offset(x, 1, 1), Blocks.SMOOTH_STONE_SLAB));
        }
        
        // Раковина (котел)
        blocks.add(new BlockPlacement(room.start.offset(room.width - 2, 1, 1), Blocks.CAULDRON));
    }
    
    private static void furnishBedroom(List<BlockPlacement> blocks, Room room) {
        // Кровать
        BlockPos bedPos = room.start.offset(1, 0, 1);
        blocks.add(new BlockPlacement(bedPos, Blocks.RED_BED));
        blocks.add(new BlockPlacement(bedPos.offset(1, 0, 0), Blocks.RED_BED));
        
        // Шкаф (сундук)
        blocks.add(new BlockPlacement(room.start.offset(room.width - 2, 0, 1), Blocks.CHEST));
        
        // Стол (верстак)
        blocks.add(new BlockPlacement(room.start.offset(1, 0, room.depth - 2), Blocks.CRAFTING_TABLE));
        
        // Стул (ступенька)
        blocks.add(new BlockPlacement(room.start.offset(2, 0, room.depth - 2), Blocks.OAK_STAIRS));
    }
    
    private static void furnishBathroom(List<BlockPlacement> blocks, Room room) {
        // Ванна (используем кварцевые блоки)
        BlockPos bathPos = room.start.offset(1, 0, 1);
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 3; z++) {
                blocks.add(new BlockPlacement(bathPos.offset(x, 0, z), Blocks.QUARTZ_BLOCK));
            }
        }
        
        // Раковина
        blocks.add(new BlockPlacement(room.start.offset(room.width - 2, 1, 1), Blocks.CAULDRON));
        
        // Зеркало (стекло)
        blocks.add(new BlockPlacement(room.start.offset(room.width - 1, 2, 1), Blocks.GLASS));
    }
    
    private static void furnishStorage(List<BlockPlacement> blocks, Room room) {
        // Полки с сундуками
        for (int x = 1; x < room.width - 1; x += 2) {
            for (int z = 1; z < room.depth - 1; z += 2) {
                blocks.add(new BlockPlacement(room.start.offset(x, 0, z), Blocks.CHEST));
                // Полка сверху
                if (room.height > 2) {
                    blocks.add(new BlockPlacement(room.start.offset(x, 2, z), Blocks.CHEST));
                }
            }
        }
        
        // Бочки для дополнительного хранения
        blocks.add(new BlockPlacement(room.start.offset(1, 0, room.depth - 2), Blocks.BARREL));
        blocks.add(new BlockPlacement(room.start.offset(2, 0, room.depth - 2), Blocks.BARREL));
    }
    
    // Детальная крыша
    private static void buildDetailedRoof(List<BlockPlacement> blocks, BlockPos start, int width, int height, int depth, Block roofMaterial) {
        // Двускатная крыша
        int roofStart = height;
        int roofPeak = height + Math.min(width, depth) / 2;
        
        for (int y = roofStart; y <= roofPeak; y++) {
            int inset = y - roofStart;
            
            // Скаты крыши
            for (int x = inset; x < width - inset; x++) {
                blocks.add(new BlockPlacement(start.offset(x, y, inset), roofMaterial));
                blocks.add(new BlockPlacement(start.offset(x, y, depth - 1 - inset), roofMaterial));
            }
            
            // Боковые части крыши
            for (int z = inset + 1; z < depth - 1 - inset; z++) {
                if (inset < width / 2) {
                    blocks.add(new BlockPlacement(start.offset(inset, y, z), roofMaterial));
                    blocks.add(new BlockPlacement(start.offset(width - 1 - inset, y, z), roofMaterial));
                }
            }
        }
        
        // Дымоход
        BlockPos chimneyPos = start.offset(width / 4, roofStart, depth / 4);
        for (int y = 0; y < 4; y++) {
            blocks.add(new BlockPlacement(chimneyPos.offset(0, y, 0), Blocks.BRICK_STAIRS));
            blocks.add(new BlockPlacement(chimneyPos.offset(1, y, 0), Blocks.BRICK_STAIRS));
            blocks.add(new BlockPlacement(chimneyPos.offset(0, y, 1), Blocks.BRICK_STAIRS));
            blocks.add(new BlockPlacement(chimneyPos.offset(1, y, 1), Blocks.BRICK_STAIRS));
        }
    }
    
    // Добавление окон
    private static void addWindows(List<BlockPlacement> blocks, BlockPos start, int width, int height, int depth) {
        // Окна на первом этаже
        for (int x = 2; x < width - 2; x += 3) {
            blocks.add(new BlockPlacement(start.offset(x, 2, 0), Blocks.GLASS_PANE));
            blocks.add(new BlockPlacement(start.offset(x, 2, depth - 1), Blocks.GLASS_PANE));
        }
        
        for (int z = 2; z < depth - 2; z += 3) {
            blocks.add(new BlockPlacement(start.offset(0, 2, z), Blocks.GLASS_PANE));
            blocks.add(new BlockPlacement(start.offset(width - 1, 2, z), Blocks.GLASS_PANE));
        }
        
        // Окна на втором этаже (если есть)
        if (height > 4) {
            for (int x = 2; x < width - 2; x += 3) {
                blocks.add(new BlockPlacement(start.offset(x, 5, 0), Blocks.GLASS_PANE));
                blocks.add(new BlockPlacement(start.offset(x, 5, depth - 1), Blocks.GLASS_PANE));
            }
        }
    }
    
    // Главный вход
    private static void addMainEntrance(List<BlockPlacement> blocks, BlockPos start, int width, int height, int depth) {
        // Одинарная дверь в центре передней стены
        int doorX = width / 2;
        BlockPos doorPos = start.offset(doorX, 1, 0);
        
        // Убираем блоки стены для двери
        blocks.removeIf(bp -> bp.pos.equals(doorPos) || bp.pos.equals(doorPos.above()));
        
        // Добавляем дверь (нижняя и верхняя части)
        blocks.add(new BlockPlacement(doorPos, Blocks.OAK_DOOR));
        blocks.add(new BlockPlacement(doorPos.above(), Blocks.OAK_DOOR));
        
        // Крыльцо
        for (int x = doorX - 1; x <= doorX + 1; x++) {
            for (int z = -2; z <= -1; z++) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.STONE_BRICKS));
            }
        }
        
        // Ступеньки к двери
        blocks.add(new BlockPlacement(start.offset(doorX, 0, -1), Blocks.OAK_STAIRS));
    }
    
    // Особняк с множеством комнат
    private static List<BlockPlacement> buildMansion(BlockPos start, int width, int height, int depth, List<Block> materials) {
        List<BlockPlacement> blocks = new ArrayList<>();
        
        // Особняк - это большой дом с дополнительными элементами
        blocks.addAll(buildDetailedHouse(start, width, height, depth, materials));
        
        // Добавляем крылья к особняку
        if (width > 12 && depth > 12) {
            // Левое крыло
            blocks.addAll(buildDetailedHouse(start.offset(-8, 0, 0), 8, height - 1, depth / 2, materials));
            
            // Правое крыло
            blocks.addAll(buildDetailedHouse(start.offset(width, 0, 0), 8, height - 1, depth / 2, materials));
            
            // Задний сад с беседкой
            buildGazebo(blocks, start.offset(width / 2 - 2, 0, depth + 5), materials);
        }
        
        return blocks;
    }
    
    // Беседка для сада
    private static void buildGazebo(List<BlockPlacement> blocks, BlockPos start, List<Block> materials) {
        Block pillarMaterial = Blocks.OAK_LOG;
        Block roofMaterial = Blocks.DARK_OAK_PLANKS;
        
        // Столбы беседки
        for (int y = 0; y < 4; y++) {
            blocks.add(new BlockPlacement(start.offset(0, y, 0), pillarMaterial));
            blocks.add(new BlockPlacement(start.offset(4, y, 0), pillarMaterial));
            blocks.add(new BlockPlacement(start.offset(0, y, 4), pillarMaterial));
            blocks.add(new BlockPlacement(start.offset(4, y, 4), pillarMaterial));
        }
        
        // Крыша беседки
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                if (x == 0 || x == 4 || z == 0 || z == 4) {
                    blocks.add(new BlockPlacement(start.offset(x, 4, z), roofMaterial));
                }
            }
        }
        
        // Скамейки
        blocks.add(new BlockPlacement(start.offset(1, 1, 1), Blocks.OAK_STAIRS));
        blocks.add(new BlockPlacement(start.offset(3, 1, 1), Blocks.OAK_STAIRS));
        blocks.add(new BlockPlacement(start.offset(1, 1, 3), Blocks.OAK_STAIRS));
        blocks.add(new BlockPlacement(start.offset(3, 1, 3), Blocks.OAK_STAIRS));
    }
    
    // Многоквартирный дом
    private static List<BlockPlacement> buildApartmentBuilding(BlockPos start, int width, int height, int depth, List<Block> materials) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block wallMaterial = getMaterial(materials, 0, Blocks.BRICK_STAIRS);
        Block floorMaterial = getMaterial(materials, 1, Blocks.OAK_PLANKS);
        
        // Строим этажи (каждые 4 блока - этаж)
        for (int floor = 0; floor < height / 4; floor++) {
            int floorY = floor * 4;
            
            // Пол этажа
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    blocks.add(new BlockPlacement(start.offset(x, floorY, z), floorMaterial));
                }
            }
            
            // Стены этажа
            for (int y = 1; y < 4; y++) {
                for (int x = 0; x < width; x++) {
                    blocks.add(new BlockPlacement(start.offset(x, floorY + y, 0), wallMaterial));
                    blocks.add(new BlockPlacement(start.offset(x, floorY + y, depth - 1), wallMaterial));
                }
                for (int z = 1; z < depth - 1; z++) {
                    blocks.add(new BlockPlacement(start.offset(0, floorY + y, z), wallMaterial));
                    blocks.add(new BlockPlacement(start.offset(width - 1, floorY + y, z), wallMaterial));
                }
            }
            
            // Квартиры на этаже
            buildApartment(blocks, start.offset(1, floorY + 1, 1), width / 2 - 1, 3, depth / 2 - 1);
            buildApartment(blocks, start.offset(width / 2, floorY + 1, 1), width / 2 - 1, 3, depth / 2 - 1);
            buildApartment(blocks, start.offset(1, floorY + 1, depth / 2), width / 2 - 1, 3, depth / 2 - 1);
            buildApartment(blocks, start.offset(width / 2, floorY + 1, depth / 2), width / 2 - 1, 3, depth / 2 - 1);
        }
        
        // Лестничная клетка
        buildStairwell(blocks, start.offset(width / 2 - 1, 0, depth / 2 - 1), height);
        
        return blocks;
    }
    
    // Детальный замок
    private static List<BlockPlacement> buildDetailedCastle(BlockPos start, int width, int height, int depth, List<Block> materials) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block stoneMaterial = Blocks.STONE_BRICKS;
        Block wallMaterial = Blocks.COBBLESTONE;
        
        // Основные стены
        buildBasicWalls(blocks, start, width, height, depth, wallMaterial);
        
        // Башни по углам
        buildCastleTower(blocks, start.offset(0, 0, 0), 5, height + 4);
        buildCastleTower(blocks, start.offset(width - 5, 0, 0), 5, height + 4);
        buildCastleTower(blocks, start.offset(0, 0, depth - 5), 5, height + 4);
        buildCastleTower(blocks, start.offset(width - 5, 0, depth - 5), 5, height + 4);
        
        // Главный зал
        buildThroneRoom(blocks, start.offset(width / 2 - 4, 1, depth / 2 - 3), 8, height - 1, 6);
        
        // Казармы
        buildBarracks(blocks, start.offset(2, 1, 2), width / 3, height - 1, depth / 3);
        
        return blocks;
    }
    
    // Детальная башня
    private static List<BlockPlacement> buildDetailedTower(BlockPos start, int width, int height, List<Block> materials) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block wallMaterial = getMaterial(materials, 0, Blocks.STONE_BRICKS);
        
        // Основная башня
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == width - 1);
                    if (y == 0 || isEdge) {
                        blocks.add(new BlockPlacement(start.offset(x, y, z), wallMaterial));
                    }
                }
            }
        }
        
        // Этажи башни
        for (int floor = 1; floor < height / 4; floor++) {
            int floorY = floor * 4;
            buildTowerFloor(blocks, start.offset(1, floorY, 1), width - 2, width - 2);
        }
        
        // Спиральная лестница
        buildSpiralStaircase(blocks, start.offset(width - 2, 1, width - 2), height - 1);
        
        // Крыша башни
        buildTowerRoof(blocks, start.offset(0, height, 0), width);
        
        return blocks;
    }
    
    // Вилла
    private static List<BlockPlacement> buildVilla(BlockPos start, int width, int height, int depth, List<Block> materials) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block wallMaterial = getMaterial(materials, 0, Blocks.QUARTZ_BLOCK);
        Block floorMaterial = getMaterial(materials, 1, Blocks.POLISHED_GRANITE);
        
        // Основная структура
        blocks.addAll(buildDetailedHouse(start, width, height, depth, materials));
        
        // Терраса
        buildTerrace(blocks, start.offset(-3, 0, -3), width + 6, depth + 6);
        
        // Бассейн
        if (width > 12 && depth > 12) {
            buildPool(blocks, start.offset(width + 2, 0, depth / 2 - 2), 6, 4);
        }
        
        return blocks;
    }
    
    // Коттедж
    private static List<BlockPlacement> buildCottage(BlockPos start, int width, int height, int depth, List<Block> materials) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block wallMaterial = getMaterial(materials, 0, Blocks.OAK_LOG);
        Block roofMaterial = getMaterial(materials, 1, Blocks.SPRUCE_PLANKS);
        
        // Простая структура коттеджа
        buildBasicWalls(blocks, start, width, height, depth, wallMaterial);
        
        // Уютные комнаты
        List<Room> rooms = new ArrayList<>();
        rooms.add(new Room(start.offset(1, 1, 1), width / 2, 3, depth - 2, "living"));
        rooms.add(new Room(start.offset(width / 2, 1, 1), width / 2 - 1, 3, depth / 2, "kitchen"));
        rooms.add(new Room(start.offset(width / 2, 1, depth / 2), width / 2 - 1, 3, depth / 2 - 1, "bedroom"));
        
        furnishRooms(blocks, rooms);
        
        // Соломенная крыша
        buildThatchedRoof(blocks, start, width, height, depth, roofMaterial);
        
        return blocks;
    }
    
    // Современный дом
    private static List<BlockPlacement> buildModernHouse(BlockPos start, int width, int height, int depth, List<Block> materials) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block wallMaterial = getMaterial(materials, 0, Blocks.WHITE_CONCRETE);
        Block glassMaterial = Blocks.GLASS;
        
        // Основная структура
        buildBasicWalls(blocks, start, width, height, depth, wallMaterial);
        
        // Много стекла
        for (int x = 1; x < width - 1; x += 2) {
            for (int y = 1; y < height - 1; y++) {
                blocks.add(new BlockPlacement(start.offset(x, y, 0), glassMaterial));
                blocks.add(new BlockPlacement(start.offset(x, y, depth - 1), glassMaterial));
            }
        }
        
        // Современная мебель
        buildModernInterior(blocks, start.offset(1, 1, 1), width - 2, height - 2, depth - 2);
        
        // Плоская крыша
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, height, z), wallMaterial));
            }
        }
        
        return blocks;
    }

    // Небоскреб с лифтом
    private static List<BlockPlacement> buildSkyscraper(BlockPos start, int width, int height, int depth, List<Block> materials) {
        List<BlockPlacement> blocks = new ArrayList<>();
        Block wallMaterial = Blocks.SMOOTH_STONE;
        Block glassMaterial = Blocks.GLASS;
        
        // Строим этажи
        for (int floor = 0; floor < height / 4; floor++) {
            int floorY = floor * 4;
            
            // Пол этажа
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    blocks.add(new BlockPlacement(start.offset(x, floorY, z), Blocks.SMOOTH_STONE));
                }
            }
            
            // Стены этажа
            for (int y = 1; y < 4; y++) {
                for (int x = 0; x < width; x++) {
                    // Передняя и задняя стены - много стекла
                    if (x % 2 == 0) {
                        blocks.add(new BlockPlacement(start.offset(x, floorY + y, 0), glassMaterial));
                        blocks.add(new BlockPlacement(start.offset(x, floorY + y, depth - 1), glassMaterial));
                    } else {
                        blocks.add(new BlockPlacement(start.offset(x, floorY + y, 0), wallMaterial));
                        blocks.add(new BlockPlacement(start.offset(x, floorY + y, depth - 1), wallMaterial));
                    }
                }
                
                // Боковые стены
                for (int z = 1; z < depth - 1; z++) {
                    blocks.add(new BlockPlacement(start.offset(0, floorY + y, z), wallMaterial));
                    blocks.add(new BlockPlacement(start.offset(width - 1, floorY + y, z), wallMaterial));
                }
            }
            
            // Офисная мебель на каждом этаже
            addOfficeFloor(blocks, start.offset(0, floorY, 0), width, depth);
        }
        
        // Лифтовая шахта
        buildElevatorShaft(blocks, start.offset(width / 2, 0, depth / 2), height);
        
        return blocks;
    }
    
    // Офисный этаж
    private static void addOfficeFloor(List<BlockPlacement> blocks, BlockPos start, int width, int depth) {
        // Рабочие столы
        for (int x = 2; x < width - 2; x += 3) {
            for (int z = 2; z < depth - 2; z += 3) {
                blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.CRAFTING_TABLE));
                blocks.add(new BlockPlacement(start.offset(x + 1, 1, z), Blocks.OAK_STAIRS));
            }
        }
        
        // Переговорная комната в углу
        for (int x = width - 4; x < width - 1; x++) {
            for (int z = depth - 4; z < depth - 1; z++) {
                if (x == width - 4 || z == depth - 4) {
                    blocks.add(new BlockPlacement(start.offset(x, 1, z), Blocks.GLASS_PANE));
                    blocks.add(new BlockPlacement(start.offset(x, 2, z), Blocks.GLASS_PANE));
                }
            }
        }
        
        // Стол для переговоров
        blocks.add(new BlockPlacement(start.offset(width - 3, 1, depth - 3), Blocks.DARK_OAK_SLAB));
    }
    
    // Лифтовая шахта
    private static void buildElevatorShaft(List<BlockPlacement> blocks, BlockPos start, int height) {
        // Стены шахты
        for (int y = 0; y < height; y++) {
            blocks.add(new BlockPlacement(start.offset(-1, y, -1), Blocks.IRON_BLOCK));
            blocks.add(new BlockPlacement(start.offset(1, y, -1), Blocks.IRON_BLOCK));
            blocks.add(new BlockPlacement(start.offset(-1, y, 1), Blocks.IRON_BLOCK));
            blocks.add(new BlockPlacement(start.offset(1, y, 1), Blocks.IRON_BLOCK));
        }
        
        // Лестница в шахте (имитация лифта)
        for (int y = 1; y < height; y++) {
            blocks.add(new BlockPlacement(start.offset(0, y, 0), Blocks.LADDER));
        }
        
        // Двери лифта на каждом этаже
        for (int floor = 0; floor < height / 4; floor++) {
            int floorY = floor * 4 + 1;
            blocks.add(new BlockPlacement(start.offset(-1, floorY, 0), Blocks.IRON_DOOR));
            blocks.add(new BlockPlacement(start.offset(-1, floorY + 1, 0), Blocks.IRON_DOOR));
        }
    }
    
    private static Block getMaterial(List<Block> materials, int index, Block defaultMaterial) {
        if (materials.isEmpty()) return defaultMaterial;
        return materials.get(index % materials.size());
    }
    
    // Вспомогательные методы для новых типов зданий
    private static void buildApartment(List<BlockPlacement> blocks, BlockPos start, int width, int height, int depth) {
        // Простая квартира с основной мебелью
        blocks.add(new BlockPlacement(start.offset(1, 0, 1), Blocks.RED_BED));
        blocks.add(new BlockPlacement(start.offset(width - 2, 0, 1), Blocks.CHEST));
        blocks.add(new BlockPlacement(start.offset(1, 0, depth - 2), Blocks.CRAFTING_TABLE));
    }
    
    private static void buildStairwell(List<BlockPlacement> blocks, BlockPos start, int height) {
        for (int y = 1; y < height; y++) {
            blocks.add(new BlockPlacement(start.offset(0, y, 0), Blocks.LADDER));
        }
    }
    
    private static void buildBasicWalls(List<BlockPlacement> blocks, BlockPos start, int width, int height, int depth, Block material) {
        // Пол
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), material));
            }
        }
        
        // Стены
        for (int y = 1; y < height; y++) {
            for (int x = 0; x < width; x++) {
                blocks.add(new BlockPlacement(start.offset(x, y, 0), material));
                blocks.add(new BlockPlacement(start.offset(x, y, depth - 1), material));
            }
            for (int z = 1; z < depth - 1; z++) {
                blocks.add(new BlockPlacement(start.offset(0, y, z), material));
                blocks.add(new BlockPlacement(start.offset(width - 1, y, z), material));
            }
        }
    }
    
    private static void buildCastleTower(List<BlockPlacement> blocks, BlockPos start, int size, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    boolean isEdge = (x == 0 || x == size - 1 || z == 0 || z == size - 1);
                    if (y == 0 || isEdge) {
                        blocks.add(new BlockPlacement(start.offset(x, y, z), Blocks.STONE_BRICKS));
                    }
                }
            }
        }
    }
    
    private static void buildThroneRoom(List<BlockPlacement> blocks, BlockPos start, int width, int height, int depth) {
        // Трон
        blocks.add(new BlockPlacement(start.offset(width / 2, 0, depth - 2), Blocks.QUARTZ_STAIRS));
        blocks.add(new BlockPlacement(start.offset(width / 2, 1, depth - 2), Blocks.QUARTZ_STAIRS));
        
        // Колонны
        for (int y = 0; y < height; y++) {
            blocks.add(new BlockPlacement(start.offset(2, y, 2), Blocks.STONE_BRICK_STAIRS));
            blocks.add(new BlockPlacement(start.offset(width - 3, y, 2), Blocks.STONE_BRICK_STAIRS));
        }
    }
    
    private static void buildBarracks(List<BlockPlacement> blocks, BlockPos start, int width, int height, int depth) {
        // Кровати для солдат
        for (int x = 1; x < width - 1; x += 3) {
            blocks.add(new BlockPlacement(start.offset(x, 0, 1), Blocks.BROWN_BED));
            blocks.add(new BlockPlacement(start.offset(x, 0, depth - 2), Blocks.BROWN_BED));
        }
        
        // Сундуки с оружием
        blocks.add(new BlockPlacement(start.offset(1, 0, depth / 2), Blocks.CHEST));
        blocks.add(new BlockPlacement(start.offset(width - 2, 0, depth / 2), Blocks.CHEST));
    }
    
    private static void buildTowerFloor(List<BlockPlacement> blocks, BlockPos start, int width, int depth) {
        // Пол этажа
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.OAK_PLANKS));
            }
        }
        
        // Простая мебель
        blocks.add(new BlockPlacement(start.offset(1, 1, 1), Blocks.CRAFTING_TABLE));
        blocks.add(new BlockPlacement(start.offset(width - 2, 1, 1), Blocks.CHEST));
    }
    
    private static void buildTowerRoof(List<BlockPlacement> blocks, BlockPos start, int width) {
        // Коническая крыша
        for (int layer = 0; layer < width / 2; layer++) {
            for (int x = layer; x < width - layer; x++) {
                for (int z = layer; z < width - layer; z++) {
                    if (x == layer || x == width - 1 - layer || z == layer || z == width - 1 - layer) {
                        blocks.add(new BlockPlacement(start.offset(x, layer, z), Blocks.DARK_OAK_PLANKS));
                    }
                }
            }
        }
    }
    
    private static void buildTerrace(List<BlockPlacement> blocks, BlockPos start, int width, int depth) {
        // Пол террасы
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.SMOOTH_STONE));
            }
        }
        
        // Перила
        for (int x = 0; x < width; x++) {
            blocks.add(new BlockPlacement(start.offset(x, 1, 0), Blocks.OAK_FENCE));
            blocks.add(new BlockPlacement(start.offset(x, 1, depth - 1), Blocks.OAK_FENCE));
        }
        for (int z = 1; z < depth - 1; z++) {
            blocks.add(new BlockPlacement(start.offset(0, 1, z), Blocks.OAK_FENCE));
            blocks.add(new BlockPlacement(start.offset(width - 1, 1, z), Blocks.OAK_FENCE));
        }
    }
    
    private static void buildPool(List<BlockPlacement> blocks, BlockPos start, int width, int depth) {
        // Бассейн
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockPlacement(start.offset(x, -1, z), Blocks.LIGHT_BLUE_CONCRETE));
                blocks.add(new BlockPlacement(start.offset(x, 0, z), Blocks.WATER));
            }
        }
        
        // Края бассейна
        for (int x = -1; x <= width; x++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, -1), Blocks.SMOOTH_STONE));
            blocks.add(new BlockPlacement(start.offset(x, 0, depth), Blocks.SMOOTH_STONE));
        }
        for (int z = 0; z < depth; z++) {
            blocks.add(new BlockPlacement(start.offset(-1, 0, z), Blocks.SMOOTH_STONE));
            blocks.add(new BlockPlacement(start.offset(width, 0, z), Blocks.SMOOTH_STONE));
        }
    }
    
    private static void buildThatchedRoof(List<BlockPlacement> blocks, BlockPos start, int width, int height, int depth, Block material) {
        // Простая двускатная крыша
        int roofHeight = Math.min(width, depth) / 2;
        for (int y = 0; y < roofHeight; y++) {
            int inset = y;
            for (int x = inset; x < width - inset; x++) {
                blocks.add(new BlockPlacement(start.offset(x, height + y, inset), material));
                blocks.add(new BlockPlacement(start.offset(x, height + y, depth - 1 - inset), material));
            }
        }
    }
    
    private static void buildModernInterior(List<BlockPlacement> blocks, BlockPos start, int width, int height, int depth) {
        // Современная мебель
        blocks.add(new BlockPlacement(start.offset(1, 0, 1), Blocks.QUARTZ_STAIRS));
        blocks.add(new BlockPlacement(start.offset(2, 0, 1), Blocks.QUARTZ_STAIRS));
        blocks.add(new BlockPlacement(start.offset(width - 2, 0, 1), Blocks.SMOOTH_STONE_SLAB));
        
        // Кухонный остров
        for (int x = width / 2 - 1; x <= width / 2 + 1; x++) {
            blocks.add(new BlockPlacement(start.offset(x, 0, depth / 2), Blocks.QUARTZ_SLAB));
        }
    }
    
    private static void buildSpiralStaircase(List<BlockPlacement> blocks, BlockPos start, int height) {
        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        
        for (int y = 0; y < height; y++) {
            Direction dir = directions[y % 4];
            BlockPos stepPos = start.offset(dir.getStepX(), y, dir.getStepZ());
            blocks.add(new BlockPlacement(stepPos, Blocks.OAK_STAIRS));
        }
    }
}