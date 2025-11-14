package com.crafto.ai.structure;

import com.crafto.ai.CraftoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Конвертер для преобразования schematic файлов в NBT формат Minecraft
 */
public class SchematicConverter {
    
    /**
     * Конвертирует schematic файл в NBT формат
     */
    public static boolean convertSchematicToNBT(File schematicFile, File outputNBT) {
        try {
            CraftoMod.LOGGER.info("Converting schematic {} to NBT {}", 
                schematicFile.getName(), outputNBT.getName());
            
            // Читаем schematic файл
            CompoundTag schematicData = NbtIo.readCompressed(new FileInputStream(schematicFile));
            
            // Создаем NBT структуру в формате Minecraft
            CompoundTag nbtStructure = new CompoundTag();
            
            // Получаем размеры
            short width = schematicData.getShort("Width");
            short height = schematicData.getShort("Height");
            short length = schematicData.getShort("Length");
            
            ListTag sizeList = new ListTag();
            sizeList.add(net.minecraft.nbt.IntTag.valueOf(width));
            sizeList.add(net.minecraft.nbt.IntTag.valueOf(height));
            sizeList.add(net.minecraft.nbt.IntTag.valueOf(length));
            nbtStructure.put("size", sizeList);
            
            // Получаем данные блоков из schematic
            byte[] blocks = schematicData.getByteArray("Blocks");
            byte[] blockData = schematicData.getByteArray("Data");
            
            // Создаем палитру блоков
            Map<Integer, Block> blockPalette = new HashMap<>();
            List<CompoundTag> paletteList = new ArrayList<>();
            int paletteIndex = 0;
            
            // Создаем список блоков для структуры
            ListTag blocksList = new ListTag();
            
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        int index = y * width * length + z * width + x;
                        int blockId = blocks[index] & 0xFF;
                        
                        if (blockId == 0) continue; // Пропускаем воздух
                        
                        // Конвертируем старый ID блока в современный блок
                        Block block = convertLegacyBlockId(blockId);
                        
                        // Добавляем в палитру если еще нет
                        if (!blockPalette.containsValue(block)) {
                            blockPalette.put(paletteIndex, block);
                            
                            CompoundTag paletteEntry = new CompoundTag();
                            paletteEntry.putString("Name", getBlockName(block));
                            paletteList.add(paletteEntry);
                            paletteIndex++;
                        }
                        
                        // Находим индекс блока в палитре
                        int stateIndex = getBlockPaletteIndex(blockPalette, block);
                        
                        // Создаем запись блока
                        CompoundTag blockTag = new CompoundTag();
                        blockTag.putInt("state", stateIndex);
                        
                        ListTag posList = new ListTag();
                        posList.add(net.minecraft.nbt.IntTag.valueOf(x));
                        posList.add(net.minecraft.nbt.IntTag.valueOf(y));
                        posList.add(net.minecraft.nbt.IntTag.valueOf(z));
                        blockTag.put("pos", posList);
                        
                        blocksList.add(blockTag);
                    }
                }
            }
            
            // Добавляем палитру и блоки в структуру
            ListTag paletteTag = new ListTag();
            paletteTag.addAll(paletteList);
            nbtStructure.put("palette", paletteTag);
            nbtStructure.put("blocks", blocksList);
            
            // Сохраняем NBT файл
            NbtIo.writeCompressed(nbtStructure, new FileOutputStream(outputNBT));
            
            CraftoMod.LOGGER.info("Successfully converted schematic to NBT: {} blocks, {}x{}x{}", 
                blocksList.size(), width, height, length);
            
            return true;
            
        } catch (IOException e) {
            CraftoMod.LOGGER.error("Failed to convert schematic to NBT", e);
            return false;
        }
    }
    
    /**
     * Конвертирует старый ID блока в современный Block
     */
    private static Block convertLegacyBlockId(int legacyId) {
        return switch (legacyId) {
            case 1 -> Blocks.STONE;
            case 2 -> Blocks.GRASS_BLOCK;
            case 3 -> Blocks.DIRT;
            case 4 -> Blocks.COBBLESTONE;
            case 5 -> Blocks.OAK_PLANKS;
            case 17 -> Blocks.OAK_LOG;
            case 20 -> Blocks.GLASS;
            case 24 -> Blocks.SANDSTONE;
            case 35 -> Blocks.WHITE_WOOL;
            case 43 -> Blocks.STONE_SLAB;
            case 44 -> Blocks.STONE_SLAB;
            case 45 -> Blocks.BRICKS;
            case 53 -> Blocks.OAK_STAIRS;
            case 64 -> Blocks.OAK_DOOR;
            case 67 -> Blocks.COBBLESTONE_STAIRS;
            case 85 -> Blocks.OAK_FENCE;
            case 98 -> Blocks.STONE_BRICKS;
            case 101 -> Blocks.IRON_BARS;
            case 102 -> Blocks.GLASS_PANE;
            case 109 -> Blocks.STONE_BRICK_STAIRS;
            case 155 -> Blocks.QUARTZ_BLOCK;
            case 156 -> Blocks.QUARTZ_STAIRS;
            default -> Blocks.STONE;
        };
    }
    
    /**
     * Получает имя блока для NBT
     */
    private static String getBlockName(Block block) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
    }
    
    /**
     * Находит индекс блока в палитре
     */
    private static int getBlockPaletteIndex(Map<Integer, Block> palette, Block block) {
        for (Map.Entry<Integer, Block> entry : palette.entrySet()) {
            if (entry.getValue() == block) {
                return entry.getKey();
            }
        }
        return 0;
    }
    
    /**
     * Конвертирует все schematic файлы в папке structures
     */
    public static void convertAllSchematics() {
        File structuresDir = new File("structures");
        if (!structuresDir.exists() || !structuresDir.isDirectory()) {
            CraftoMod.LOGGER.warn("Structures directory not found");
            return;
        }
        
        File[] schematicFiles = structuresDir.listFiles((dir, name) -> 
            name.endsWith(".schematic"));
        
        if (schematicFiles == null || schematicFiles.length == 0) {
            CraftoMod.LOGGER.info("No schematic files found to convert");
            return;
        }
        
        for (File schematicFile : schematicFiles) {
            String baseName = schematicFile.getName().replace(".schematic", "");
            File outputNBT = new File(structuresDir, baseName + ".nbt");
            
            if (outputNBT.exists()) {
                CraftoMod.LOGGER.info("NBT file already exists for {}, skipping", baseName);
                continue;
            }
            
            convertSchematicToNBT(schematicFile, outputNBT);
        }
    }
}
