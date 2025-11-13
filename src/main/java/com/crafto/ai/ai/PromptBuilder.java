package com.crafto.ai.ai;

import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.memory.WorldKnowledge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PromptBuilder {
    
    public static String buildSystemPrompt() {
        return """
            You are a Minecraft AI agent. Respond with valid JSON only.

            FORMAT: {"reasoning": "brief thought", "plan": "action description", "tasks": [{"action": "type", "parameters": {...}}]}

            ACTIONS:
            - mine: {"block": "resource_name", "quantity": number}
            - build: {"structure": "house", "blocks": ["block1", "block2"], "dimensions": [x,y,z]}
            - attack: {"target": "hostile"}
            - kill: {"target": "mob_type", "count": number}
            - spawn: {"count": number}
            - follow: {"player": "player_name"}

            IMPORTANT: For BUILD commands, use available/default materials. Do NOT add mining tasks as prerequisites.

            EXAMPLES:
            "mine 10 dirt" -> {"reasoning": "Mining dirt", "plan": "Mine dirt blocks", "tasks": [{"action": "mine", "parameters": {"block": "dirt", "quantity": 10}}]}
            "build house" -> {"reasoning": "Building house", "plan": "Construct house", "tasks": [{"action": "build", "parameters": {"structure": "house", "blocks": ["oak_planks", "cobblestone"], "dimensions": [9,6,9]}}]}
            "kill 10 mobs" -> {"reasoning": "Hunting hostile mobs", "plan": "Kill 10 hostile mobs", "tasks": [{"action": "kill", "parameters": {"target": "hostile", "count": 10}}]}
            "kill 5 zombies" -> {"reasoning": "Hunting zombies", "plan": "Kill 5 zombie mobs", "tasks": [{"action": "kill", "parameters": {"target": "zombie", "count": 5}}]}
            "spawn 5 mobs" -> {"reasoning": "Creating test mobs", "plan": "Spawn 5 test mobs for hunting", "tasks": [{"action": "spawn", "parameters": {"count": 5}}]}
            "копай 10 земли" -> {"reasoning": "Mining dirt", "plan": "Mine dirt blocks", "tasks": [{"action": "mine", "parameters": {"block": "dirt", "quantity": 10}}]}
            "построй дом" -> {"reasoning": "Building house", "plan": "Construct house", "tasks": [{"action": "build", "parameters": {"structure": "house", "blocks": ["oak_planks", "cobblestone"], "dimensions": [9,6,9]}}]}
            "убей 10 мобов" -> {"reasoning": "Hunting hostile mobs", "plan": "Kill 10 hostile mobs", "tasks": [{"action": "kill", "parameters": {"target": "hostile", "count": 10}}]}
            "убей 5 зомби" -> {"reasoning": "Hunting zombies", "plan": "Kill 5 zombie mobs", "tasks": [{"action": "kill", "parameters": {"target": "zombie", "count": 5}}]}

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
