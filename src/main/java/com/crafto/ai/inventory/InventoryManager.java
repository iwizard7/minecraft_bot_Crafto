package com.crafto.ai.inventory;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.entity.CraftoEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryManager {
    private static final int MIN_BLOCKS_THRESHOLD = 50; // Пополнять когда меньше 50 блоков
    private static final int REFILL_AMOUNT = 1000; // Пополнять на 1000 блоков (неограниченно для ботов)
    
    // Виртуальный инвентарь для каждого Crafto (UUID -> Block -> количество)
    private static final Map<String, Map<Block, Integer>> VIRTUAL_INVENTORIES = new ConcurrentHashMap<>();
    
    /**
     * Получает виртуальный инвентарь Crafto
     */
    private static Map<Block, Integer> getInventory(CraftoEntity crafto) {
        String craftoId = crafto.getUUID().toString();
        return VIRTUAL_INVENTORIES.computeIfAbsent(craftoId, k -> new HashMap<>());
    }
    
    /**
     * Проверяет нужно ли пополнить инвентарь для строительства
     */
    public static boolean needsRefill(CraftoEntity crafto, Block requiredBlock) {
        int totalCount = getBlockCount(crafto, requiredBlock);
        boolean needsRefill = totalCount < MIN_BLOCKS_THRESHOLD;
        
        if (needsRefill) {
            CraftoMod.LOGGER.info("Crafto '{}' needs refill: {} blocks of {} (threshold: {})", 
                crafto.getCraftoName(), totalCount, requiredBlock, MIN_BLOCKS_THRESHOLD);
        }
        
        return needsRefill;
    }
    
    /**
     * Пополняет инвентарь указанным блоком
     */
    public static void refillInventory(CraftoEntity crafto, Block block) {
        Map<Block, Integer> inventory = getInventory(crafto);
        int currentCount = inventory.getOrDefault(block, 0);
        int neededAmount = REFILL_AMOUNT - currentCount;
        
        if (neededAmount <= 0) {
            return; // Уже достаточно блоков
        }
        
        // Добавляем блоки в виртуальный инвентарь
        inventory.put(block, currentCount + neededAmount);
        
        CraftoMod.LOGGER.info("Crafto '{}' refilled inventory: +{} {} (total: {})", 
            crafto.getCraftoName(), neededAmount, block, inventory.get(block));
    }
    
    /**
     * Пополняет инвентарь всеми необходимыми материалами для строительства
     */
    public static void refillBuildingMaterials(CraftoEntity crafto, java.util.List<Block> materials) {
        for (Block block : materials) {
            if (needsRefill(crafto, block)) {
                refillInventory(crafto, block);
            }
        }
    }
    
    /**
     * Получает количество блоков в виртуальном инвентаре
     */
    public static int getBlockCount(CraftoEntity crafto, Block block) {
        Map<Block, Integer> inventory = getInventory(crafto);
        return inventory.getOrDefault(block, 0);
    }
    
    /**
     * Проверяет есть ли в инвентаре нужный блок
     */
    public static boolean hasBlock(CraftoEntity crafto, Block block) {
        return getBlockCount(crafto, block) > 0;
    }
    
    /**
     * Потребляет один блок из виртуального инвентаря
     */
    public static boolean consumeBlock(CraftoEntity crafto, Block block) {
        Map<Block, Integer> inventory = getInventory(crafto);
        int currentCount = inventory.getOrDefault(block, 0);
        
        if (currentCount > 0) {
            inventory.put(block, currentCount - 1);
            return true;
        }
        
        return false;
    }
    
    /**
     * Получает статистику виртуального инвентаря для отладки
     */
    public static String getInventoryStats(CraftoEntity crafto) {
        Map<Block, Integer> inventory = getInventory(crafto);
        
        if (inventory.isEmpty()) {
            return "Virtual Inventory: empty";
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("Virtual Inventory: ");
        for (Map.Entry<Block, Integer> entry : inventory.entrySet()) {
            if (entry.getValue() > 0) {
                stats.append(entry.getKey().toString().replace("minecraft:", ""))
                     .append("x").append(entry.getValue()).append(" ");
            }
        }
        
        return stats.toString();
    }
    
    /**
     * Очищает виртуальный инвентарь Crafto (для отладки)
     */
    public static void clearInventory(CraftoEntity crafto) {
        String craftoId = crafto.getUUID().toString();
        VIRTUAL_INVENTORIES.remove(craftoId);
        CraftoMod.LOGGER.info("Cleared virtual inventory for Crafto '{}'", crafto.getCraftoName());
    }
}