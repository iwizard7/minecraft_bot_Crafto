package com.crafto.ai.ai;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.action.Task;
import com.crafto.ai.config.CraftoConfig;
import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.memory.WorldKnowledge;
import com.crafto.ai.optimization.PerformanceManager;
import com.crafto.ai.memory.AgentMemory;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TaskPlanner {
    private final OllamaClient ollamaClient;
    private final PerformanceManager performanceManager;

    public TaskPlanner() {
        this.ollamaClient = new OllamaClient();
        this.performanceManager = PerformanceManager.getInstance();
    }

    public ResponseParser.ParsedResponse planTasks(CraftoEntity crafto, String command) {
        return planTasksAsync(crafto, command).join();
    }
    
    public CompletableFuture<ResponseParser.ParsedResponse> planTasksAsync(CraftoEntity crafto, String command) {
        long startTime = System.currentTimeMillis();
        String agentName = crafto.getCraftoName();
        
        // Проверяем готовность агента
        if (!performanceManager.isAgentReady(agentName, command)) {
            CraftoMod.LOGGER.warn("Agent {} is not ready for task: {}", agentName, command);
            return CompletableFuture.completedFuture(null);
        }
        
        // Получаем память агента
        AgentMemory memory = performanceManager.getAgentMemory(agentName);
        
        // Проверяем оптимальную стратегию
        Optional<String> optimalStrategy = performanceManager.getOptimalStrategy(agentName, command);
        if (optimalStrategy.isPresent()) {
            CraftoMod.LOGGER.info("Using optimal strategy for {}: {}", command, optimalStrategy.get());
        }
        
        // Записываем информацию о локации
        BlockPos pos = crafto.blockPosition();
        String biome = crafto.level().getBiome(pos).toString();
        memory.recordLocation(pos, biome, List.of());
        
        try {
            String systemPrompt = PromptBuilder.buildSystemPrompt();
            WorldKnowledge worldKnowledge = new WorldKnowledge(crafto);
            String userPrompt = PromptBuilder.buildUserPrompt(crafto, command, worldKnowledge);
            
            // Добавляем контекст из памяти
            String enhancedPrompt = enhancePromptWithMemory(userPrompt, memory, command);
            
            CraftoMod.LOGGER.info("Requesting AI plan for Crafto '{}' using Ollama: {}", agentName, command);
            
            // Используем только Ollama
            return performanceManager.processAIRequest(agentName, command, enhancedPrompt)
                .thenApply(response -> {
                    if (response == null) {
                        CraftoMod.LOGGER.error("Failed to get AI response for command: {}", command);
                        return null;
                    }
                    
                    ResponseParser.ParsedResponse parsedResponse = ResponseParser.parseAIResponse(response);
                    
                    if (parsedResponse == null) {
                        CraftoMod.LOGGER.error("Failed to parse AI response");
                        return null;
                    }
                    
                    long executionTime = System.currentTimeMillis() - startTime;
                    
                    // Записываем стратегию
                    performanceManager.recordStrategy(agentName, command, 
                        parsedResponse.getPlan(), executionTime, true);
                    
                    CraftoMod.LOGGER.info("Plan: {} ({} tasks) - completed in {}ms", 
                        parsedResponse.getPlan(), parsedResponse.getTasks().size(), executionTime);
                    
                    return parsedResponse;
                })
                .exceptionally(throwable -> {
                    long executionTime = System.currentTimeMillis() - startTime;
                    performanceManager.recordStrategy(agentName, command, "failed", executionTime, false);
                    CraftoMod.LOGGER.error("Error planning tasks for " + agentName, throwable);
                    return null;
                });
            
        } catch (Exception e) {
            CraftoMod.LOGGER.error("Error planning tasks", e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private String enhancePromptWithMemory(String basePrompt, AgentMemory memory, String command) {
        StringBuilder enhanced = new StringBuilder(basePrompt);
        
        // Добавляем информацию о предыдущих успешных стратегиях
        Optional<AgentMemory.SuccessfulStrategy> bestStrategy = memory.getBestStrategy(command);
        if (bestStrategy.isPresent()) {
            enhanced.append("\n\nPrevious successful strategy for this task: ")
                    .append(bestStrategy.get().strategy)
                    .append(" (Success rate: ")
                    .append(String.format("%.1f%%", bestStrategy.get().successRate * 100))
                    .append(")");
        }
        
        // Добавляем информацию об изученных локациях
        List<AgentMemory.LocationInfo> locations = memory.getExploredLocations();
        if (!locations.isEmpty()) {
            enhanced.append("\n\nKnown locations: ");
            locations.stream().limit(3).forEach(loc -> 
                enhanced.append(String.format("(%d,%d,%d) - %s; ", 
                    loc.position.getX(), loc.position.getY(), loc.position.getZ(), loc.biome)));
        }
        
        return enhanced.toString();
    }

    private String getAIResponse(String systemPrompt, String userPrompt) {
        return ollamaClient.sendRequest(systemPrompt, userPrompt);
    }

    public boolean validateTask(Task task) {
        String action = task.getAction();
        
        return switch (action) {
            case "pathfind" -> task.hasParameters("x", "y", "z");
            case "mine" -> task.hasParameters("block", "quantity");
            case "place" -> task.hasParameters("block", "x", "y", "z");
            case "craft" -> task.hasParameters("item", "quantity");
            case "attack" -> task.hasParameters("target");
            case "follow" -> task.hasParameters("player");
            case "gather" -> task.hasParameters("resource", "quantity");
            case "build" -> task.hasParameters("structure", "blocks", "dimensions");
            // Новые действия для системы исследования
            case "explore" -> true; // Может работать с текущей позицией по умолчанию
            case "create_waypoint" -> true; // Может использовать текущую позицию и автогенерированное имя
            case "navigate_to_waypoint" -> task.hasParameters("waypoint");
            case "find_resource" -> task.hasParameters("resource");
            case "create_map" -> true; // Может работать с текущей позицией по умолчанию
            default -> {
                CraftoMod.LOGGER.warn("Unknown action type: {}", action);
                yield false;
            }
        };
    }

    public List<Task> validateAndFilterTasks(List<Task> tasks) {
        return tasks.stream()
            .filter(this::validateTask)
            .toList();
    }
}
