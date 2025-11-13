package com.crafto.ai.debug;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.action.Task;
import com.crafto.ai.ai.ResponseParser;
import com.crafto.ai.entity.CraftoEntity;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug utility for testing building commands without AI dependency
 */
public class BuildingDebugger {
    
    public static ResponseParser.ParsedResponse createBuildResponse(String command) {
        CraftoMod.LOGGER.info("Creating debug build response for: {}", command);
        
        // Парсим команду строительства
        String[] parts = command.toLowerCase().split("\\s+");
        String structureType = "house"; // по умолчанию
        int width = 12, height = 8, depth = 10; // размеры по умолчанию
        
        // Ищем тип структуры
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("build") && i + 1 < parts.length) {
                structureType = parts[i + 1];
                
                // Ищем размеры
                if (i + 2 < parts.length && isNumeric(parts[i + 2])) {
                    width = Integer.parseInt(parts[i + 2]);
                }
                if (i + 3 < parts.length && isNumeric(parts[i + 3])) {
                    height = Integer.parseInt(parts[i + 3]);
                }
                if (i + 4 < parts.length && isNumeric(parts[i + 4])) {
                    depth = Integer.parseInt(parts[i + 4]);
                }
                break;
            }
        }
        
        CraftoMod.LOGGER.info("Parsed build command: type={}, size={}x{}x{}", structureType, width, height, depth);
        
        // Создаем задачу строительства
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("structure", structureType);
        parameters.put("width", width);
        parameters.put("height", height);
        parameters.put("depth", depth);
        parameters.put("materials", List.of("oak_planks", "stone", "glass"));
        
        Task buildTask = new Task("build", parameters);
        List<Task> tasks = new ArrayList<>();
        tasks.add(buildTask);
        
        String plan = String.format("Build a %s (%dx%dx%d)", structureType, width, height, depth);
        
        return new ResponseParser.ParsedResponse(
            "Building structure as requested",
            plan,
            tasks
        );
    }
    
    public static boolean isBuildCommand(String command) {
        String lower = command.toLowerCase();
        return lower.contains("build") || lower.contains("construct") || 
               lower.contains("create") && (lower.contains("house") || lower.contains("tower") || 
               lower.contains("castle") || lower.contains("building"));
    }
    
    private static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // Тестирование строительства без AI
    public static void testBuildingSystem(CraftoEntity crafto, String command) {
        CraftoMod.LOGGER.info("Testing building system for Crafto '{}' with command: {}", crafto.getCraftoName(), command);
        
        try {
            ResponseParser.ParsedResponse response = createBuildResponse(command);
            
            if (response != null && !response.getTasks().isEmpty()) {
                CraftoMod.LOGGER.info("Created build response: {} with {} tasks", response.getPlan(), response.getTasks().size());
                
                // Выполняем задачу напрямую
                Task buildTask = response.getTasks().get(0);
                CraftoMod.LOGGER.info("Executing build task: {}", buildTask);
                
                // Создаем действие строительства
                com.crafto.ai.action.actions.BuildStructureAction buildAction = 
                    new com.crafto.ai.action.actions.BuildStructureAction(crafto, buildTask);
                
                CraftoMod.LOGGER.info("Starting build action...");
                buildAction.start();
                
                // Проверяем статус
                CraftoMod.LOGGER.info("Build action started. Is complete: {}", buildAction.isComplete());
                
            } else {
                CraftoMod.LOGGER.error("Failed to create build response");
            }
            
        } catch (Exception e) {
            CraftoMod.LOGGER.error("Error in building test", e);
        }
    }
}