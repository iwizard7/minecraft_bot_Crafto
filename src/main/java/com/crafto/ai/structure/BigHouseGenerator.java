package com.crafto.ai.structure;

import com.crafto.ai.CraftoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Генератор большого дома для создания NBT шаблона
 */
public class BigHouseGenerator {
    
    private static class BlockEntry {
        String blockName;
        int x, y, z;
        
        BlockEntry(String blockName, int x, int y, int z) {
            this.blockName = blockName;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    
    /**
     * Генерирует NBT файл для большого дома
     */
    public static void generateBigHouseNBT() {
        List<BlockEntry> blocks = new ArrayList<>();
        
        // Размеры большого дома
        int width = 15;
        int height = 8;
        int depth = 12;
        
        // Материалы
        String floor = "minecraft:oak_planks";
        String wall = "minecraft:oak_planks";
        String roof = "minecraft:dark_oak_planks";
        String window = "minecraft:glass_pane";
        String door = "minecraft:oak_door";
        String stone = "minecraft:stone_bricks";
        
        // 1. ФУНДАМЕНТ И ПОЛ
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockEntry(floor, x, 0, z));
            }
        }
        
        // 2. СТЕНЫ (3 этажа)
        for (int y = 1; y <= 6; y++) {
            for (int x = 0; x < width; x++) {
                // Передняя стена
                if (x == width / 2 && y <= 2) {
                    // Дверь
                    blocks.add(new BlockEntry(door, x, y, 0));
                } else if (y >= 2 && y <= 3 && (x == 3 || x == width - 4)) {
                    // Окна первого этажа
                    blocks.add(new BlockEntry(window, x, y, 0));
                } else if (y >= 5 && y <= 6 && (x == 2 || x == width / 2 || x == width - 3)) {
                    // Окна второго этажа
                    blocks.add(new BlockEntry(window, x, y, 0));
                } else {
                    blocks.add(new BlockEntry(wall, x, y, 0));
                }
                
                // Задняя стена
                if (y >= 2 && y <= 3 && (x == 3 || x == width - 4)) {
                    blocks.add(new BlockEntry(window, x, y, depth - 1));
                } else if (y >= 5 && y <= 6 && (x == 2 || x == width / 2 || x == width - 3)) {
                    blocks.add(new BlockEntry(window, x, y, depth - 1));
                } else {
                    blocks.add(new BlockEntry(wall, x, y, depth - 1));
                }
            }
            
            for (int z = 1; z < depth - 1; z++) {
                // Левая стена
                if (y >= 2 && y <= 3 && z % 3 == 1) {
                    blocks.add(new BlockEntry(window, 0, y, z));
                } else if (y >= 5 && y <= 6 && z % 3 == 1) {
                    blocks.add(new BlockEntry(window, 0, y, z));
                } else {
                    blocks.add(new BlockEntry(wall, 0, y, z));
                }
                
                // Правая стена
                if (y >= 2 && y <= 3 && z % 3 == 1) {
                    blocks.add(new BlockEntry(window, width - 1, y, z));
                } else if (y >= 5 && y <= 6 && z % 3 == 1) {
                    blocks.add(new BlockEntry(window, width - 1, y, z));
                } else {
                    blocks.add(new BlockEntry(wall, width - 1, y, z));
                }
            }
        }
        
        // 3. ВНУТРЕННИЕ ПЕРЕГОРОДКИ
        // Разделение на комнаты первого этажа
        for (int z = 3; z < depth - 1; z++) {
            if (z != 6) { // Оставляем проход
                blocks.add(new BlockEntry(wall, width / 2, 1, z));
                blocks.add(new BlockEntry(wall, width / 2, 2, z));
                blocks.add(new BlockEntry(wall, width / 2, 3, z));
            }
        }
        
        // 4. ВТОРОЙ ЭТАЖ (пол)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new BlockEntry(floor, x, 4, z));
            }
        }
        
        // 5. ЛЕСТНИЦА
        for (int y = 1; y <= 3; y++) {
            blocks.add(new BlockEntry("minecraft:oak_stairs", 2, y, 2));
        }
        
        // 6. КРЫША (многоуровневая)
        int roofStartHeight = 7;
        int roofLayers = 6;
        
        for (int layer = 0; layer < roofLayers; layer++) {
            int currentHeight = roofStartHeight + layer;
            int inset = layer;
            
            for (int x = inset; x < width - inset; x++) {
                for (int z = inset; z < depth - inset; z++) {
                    if (x == inset || x == width - 1 - inset || 
                        z == inset || z == depth - 1 - inset) {
                        blocks.add(new BlockEntry(roof, x, currentHeight, z));
                    }
                }
            }
            
            if (width - 2 * inset <= 1 || depth - 2 * inset <= 1) {
                break;
            }
        }
        
        // 7. ДЕКОРАТИВНЫЕ ЭЛЕМЕНТЫ
        // Колонны по углам
        for (int y = 1; y <= 6; y++) {
            blocks.add(new BlockEntry(stone, 0, y, 0));
            blocks.add(new BlockEntry(stone, width - 1, y, 0));
            blocks.add(new BlockEntry(stone, 0, y, depth - 1));
            blocks.add(new BlockEntry(stone, width - 1, y, depth - 1));
        }
        
        // 8. МЕБЕЛЬ
        // Гостиная (левая часть первого этажа)
        blocks.add(new BlockEntry("minecraft:oak_stairs", 2, 1, 2)); // Диван
        blocks.add(new BlockEntry("minecraft:oak_stairs", 3, 1, 2));
        blocks.add(new BlockEntry("minecraft:crafting_table", 2, 1, 4)); // Стол
        
        // Кухня (правая часть первого этажа)
        blocks.add(new BlockEntry("minecraft:furnace", width - 3, 1, 2));
        blocks.add(new BlockEntry("minecraft:crafting_table", width - 3, 1, 3));
        blocks.add(new BlockEntry("minecraft:chest", width - 3, 1, 4));
        
        // Спальня (второй этаж)
        blocks.add(new BlockEntry("minecraft:red_bed", 2, 5, 2));
        blocks.add(new BlockEntry("minecraft:chest", 2, 5, 4));
        
        // Сохраняем в NBT
        saveToNBT(blocks, width, height, depth, "structures/big-house.nbt");
    }
    
    /**
     * Сохраняет блоки в NBT файл
     */
    private static void saveToNBT(List<BlockEntry> blocks, int width, int height, int depth, String filename) {
        try {
            CompoundTag nbt = new CompoundTag();
            
            // Размеры
            ListTag sizeList = new ListTag();
            sizeList.add(IntTag.valueOf(width));
            sizeList.add(IntTag.valueOf(height));
            sizeList.add(IntTag.valueOf(depth));
            nbt.put("size", sizeList);
            
            // Создаем палитру уникальных блоков
            Map<String, Integer> paletteMap = new HashMap<>();
            List<String> paletteList = new ArrayList<>();
            
            for (BlockEntry block : blocks) {
                if (!paletteMap.containsKey(block.blockName)) {
                    paletteMap.put(block.blockName, paletteList.size());
                    paletteList.add(block.blockName);
                }
            }
            
            // Сохраняем палитру
            ListTag palette = new ListTag();
            for (String blockName : paletteList) {
                CompoundTag paletteEntry = new CompoundTag();
                paletteEntry.putString("Name", blockName);
                palette.add(paletteEntry);
            }
            nbt.put("palette", palette);
            
            // Сохраняем блоки
            ListTag blocksList = new ListTag();
            for (BlockEntry block : blocks) {
                CompoundTag blockTag = new CompoundTag();
                blockTag.putInt("state", paletteMap.get(block.blockName));
                
                ListTag pos = new ListTag();
                pos.add(IntTag.valueOf(block.x));
                pos.add(IntTag.valueOf(block.y));
                pos.add(IntTag.valueOf(block.z));
                blockTag.put("pos", pos);
                
                blocksList.add(blockTag);
            }
            nbt.put("blocks", blocksList);
            
            // Сохраняем файл
            File file = new File(filename);
            file.getParentFile().mkdirs();
            NbtIo.writeCompressed(nbt, new FileOutputStream(file));
            
            CraftoMod.LOGGER.info("Generated big house NBT: {} blocks, {}x{}x{} at {}", 
                blocks.size(), width, height, depth, filename);
            
        } catch (IOException e) {
            CraftoMod.LOGGER.error("Failed to save big house NBT", e);
        }
    }
    
    /**
     * Инициализация - создает NBT файл если его нет
     */
    public static void initialize() {
        File bigHouseFile = new File("structures/big-house.nbt");
        if (!bigHouseFile.exists()) {
            CraftoMod.LOGGER.info("Big house NBT not found, generating...");
            generateBigHouseNBT();
        } else {
            CraftoMod.LOGGER.info("Big house NBT already exists");
        }
    }
}
