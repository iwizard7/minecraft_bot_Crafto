package com.crafto.ai.ai;

import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.memory.WorldKnowledge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PromptBuilder {
    
    public static String buildSystemPrompt() {
        return """
            You are a Minecraft AI agent with exploration and navigation capabilities. Respond with valid JSON only.

            FORMAT: {"reasoning": "brief thought", "plan": "action description", "tasks": [{"action": "type", "parameters": {...}}]}

            BASIC ACTIONS:
            - mine: {"block": "resource_name", "quantity": number}
            - build: {"structure": "house", "blocks": ["block1", "block2"], "dimensions": [x,y,z]}
            - attack: {"target": "hostile"}
            - kill: {"target": "mob_type", "count": number}
            - spawn: {"count": number}
            - follow: {"player": "player_name"}

            EXPLORATION & NAVIGATION ACTIONS:
            - explore: {"radius": number, "x": x, "y": y, "z": z} (optional coordinates, defaults to current position)
            - create_waypoint: {"name": "waypoint_name", "type": "BASE|MINE|FARM|LANDMARK", "x": x, "y": y, "z": z, "description": "text"}
            - navigate_to_waypoint: {"waypoint": "waypoint_name"}
            - find_resource: {"resource": "diamond_ore|iron_ore|gold_ore|coal_ore"}
            - create_map: {"radius": number, "format": "text|json", "x": x, "y": y, "z": z}

            WAYPOINT TYPES:
            - BASE: Home bases, settlements
            - MINE: Mining sites, resource locations
            - FARM: Agricultural areas
            - LANDMARK: Important locations, points of interest
            - TRADING_POST: Trading locations
            - DANGER_ZONE: Dangerous areas to avoid

            IMPORTANT: For BUILD commands, use available/default materials. Do NOT add mining tasks as prerequisites.

            EXAMPLES:
            "explore area" -> {"reasoning": "Exploring nearby area", "plan": "Explore 64 block radius", "tasks": [{"action": "explore", "parameters": {"radius": 64}}]}
            "find diamonds" -> {"reasoning": "Looking for diamonds", "plan": "Find nearest diamond ore", "tasks": [{"action": "find_resource", "parameters": {"resource": "diamond_ore"}}]}
            "create waypoint home" -> {"reasoning": "Marking home location", "plan": "Create home waypoint", "tasks": [{"action": "create_waypoint", "parameters": {"name": "home", "type": "BASE", "description": "Home base"}}]}
            "go to home" -> {"reasoning": "Navigating home", "plan": "Navigate to home waypoint", "tasks": [{"action": "navigate_to_waypoint", "parameters": {"waypoint": "home"}}]}
            "create map" -> {"reasoning": "Creating area map", "plan": "Create map of surrounding area", "tasks": [{"action": "create_map", "parameters": {"radius": 200, "format": "text"}}]}
            "исследуй область" -> {"reasoning": "Exploring nearby area", "plan": "Explore 64 block radius", "tasks": [{"action": "explore", "parameters": {"radius": 64}}]}
            "найди алмазы" -> {"reasoning": "Looking for diamonds", "plan": "Find nearest diamond ore", "tasks": [{"action": "find_resource", "parameters": {"resource": "diamond_ore"}}]}
            "создай точку дом" -> {"reasoning": "Marking home location", "plan": "Create home waypoint", "tasks": [{"action": "create_waypoint", "parameters": {"name": "дом", "type": "BASE", "description": "Домашняя база"}}]}
            "иди домой" -> {"reasoning": "Navigating home", "plan": "Navigate to home waypoint", "tasks": [{"action": "navigate_to_waypoint", "parameters": {"waypoint": "дом"}}]}

            Output ONLY valid JSON.
            """;
    }

    public static String buildUserPrompt(CraftoEntity crafto, String command, WorldKnowledge worldKnowledge) {
        StringBuilder prompt = new StringBuilder();
        
        // Give agents FULL situational awareness
        prompt.append("=== YOUR SITUATION ===\n");
        prompt.append("Position: ").append(formatPosition(crafto.blockPosition())).append("\n");
        prompt.append("Nearby Players: ").append(worldKnowledge.getNearbyPlayerNames()).append("\n");
        prompt.append("Nearby Entities: ").append(worldKnowledge.getNearbyEntitiesSummary()).append("\n");
        prompt.append("Nearby Blocks: ").append(worldKnowledge.getNearbyBlocksSummary()).append("\n");
        prompt.append("Biome: ").append(worldKnowledge.getBiomeName()).append("\n");
        
        prompt.append("\n=== PLAYER COMMAND ===\n");
        prompt.append("\"").append(command).append("\"\n");
        
        prompt.append("\n=== YOUR RESPONSE (with reasoning) ===\n");
        
        return prompt.toString();
    }

    private static String formatPosition(BlockPos pos) {
        return String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ());
    }

    private static String formatInventory(CraftoEntity crafto) {
        return "[empty]";
    }
}
