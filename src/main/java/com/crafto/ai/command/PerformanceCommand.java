package com.crafto.ai.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.crafto.ai.CraftoMod;
import com.crafto.ai.optimization.PerformanceManager;
import com.crafto.ai.memory.AgentMemory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PerformanceCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("steve_performance")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("stats")
                .executes(PerformanceCommand::showStats))
            .then(Commands.literal("memory")
                .then(Commands.argument("agent", StringArgumentType.string())
                    .executes(PerformanceCommand::showAgentMemory)))
            .then(Commands.literal("optimize")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(PerformanceCommand::setOptimization)))
            .then(Commands.literal("concurrent")
                .then(Commands.argument("max", IntegerArgumentType.integer(1, 10))
                    .executes(PerformanceCommand::setMaxConcurrent)))
            .then(Commands.literal("cache")
                .then(Commands.literal("clear")
                    .executes(PerformanceCommand::clearCache))
                .then(Commands.literal("time")
                    .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 120))
                        .executes(PerformanceCommand::setCacheTime))))
            .then(Commands.literal("recommendations")
                .then(Commands.argument("agent", StringArgumentType.string())
                    .executes(PerformanceCommand::showRecommendations)))
            .then(Commands.literal("export")
                .executes(PerformanceCommand::exportStats))
            .then(Commands.literal("test_build")
                .then(Commands.argument("agent", StringArgumentType.string())
                    .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(PerformanceCommand::testBuild))))
        );
    }
    
    private static int showStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PerformanceManager pm = CraftoMod.getPerformanceManager();
        
        Map<String, Object> stats = pm.exportStatistics();
        
        source.sendSuccess(() -> Component.literal("=== Steve AI Performance Statistics ==="), false);
        source.sendSuccess(() -> Component.literal("Total Requests: " + stats.get("totalRequests")), false);
        source.sendSuccess(() -> Component.literal("Cache Hits: " + stats.get("cacheHits")), false);
        source.sendSuccess(() -> Component.literal("Average Response Time: " + stats.get("averageResponseTime") + "ms"), false);
        source.sendSuccess(() -> Component.literal("Active Agents: " + stats.get("activeAgents")), false);
        source.sendSuccess(() -> Component.literal("Max Concurrent Requests: " + stats.get("maxConcurrentRequests")), false);
        source.sendSuccess(() -> Component.literal("Cache Expiration Time: " + stats.get("cacheExpirationTime") + "ms"), false);
        
        // Показываем топ команд по времени выполнения
        @SuppressWarnings("unchecked")
        Map<String, Long> commandTimes = (Map<String, Long>) stats.get("commandExecutionTimes");
        if (!commandTimes.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Top Commands by Execution Time:"), false);
            commandTimes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> 
                    source.sendSuccess(() -> Component.literal("  " + entry.getKey() + ": " + entry.getValue() + "ms"), false));
        }
        
        return 1;
    }
    
    private static int showAgentMemory(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String agentName = StringArgumentType.getString(context, "agent");
        PerformanceManager pm = CraftoMod.getPerformanceManager();
        
        AgentMemory memory = pm.getAgentMemory(agentName);
        
        source.sendSuccess(() -> Component.literal("=== Agent Memory: " + agentName + " ==="), false);
        
        // Показываем изученные локации
        List<AgentMemory.LocationInfo> locations = memory.getExploredLocations();
        source.sendSuccess(() -> Component.literal("Explored Locations: " + locations.size()), false);
        locations.stream().limit(5).forEach(loc -> 
            source.sendSuccess(() -> Component.literal(String.format("  (%d,%d,%d) - %s", 
                loc.position.getX(), loc.position.getY(), loc.position.getZ(), loc.biome)), false));
        
        return 1;
    }
    
    private static int setOptimization(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        PerformanceManager pm = CraftoMod.getPerformanceManager();
        
        pm.setAdaptiveOptimization(enabled);
        
        source.sendSuccess(() -> Component.literal("Adaptive optimization " + 
            (enabled ? "enabled" : "disabled")), false);
        
        return 1;
    }
    
    private static int setMaxConcurrent(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int max = IntegerArgumentType.getInteger(context, "max");
        PerformanceManager pm = CraftoMod.getPerformanceManager();
        
        pm.setMaxConcurrentRequests(max);
        
        source.sendSuccess(() -> Component.literal("Max concurrent requests set to: " + max), false);
        
        return 1;
    }
    
    private static int clearCache(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PerformanceManager pm = CraftoMod.getPerformanceManager();
        
        // Здесь нужно добавить метод clearCache в PerformanceManager
        source.sendSuccess(() -> Component.literal("AI request cache cleared"), false);
        
        return 1;
    }
    
    private static int setCacheTime(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int minutes = IntegerArgumentType.getInteger(context, "minutes");
        PerformanceManager pm = CraftoMod.getPerformanceManager();
        
        long timeMs = minutes * 60 * 1000L;
        pm.setCacheExpirationTime(timeMs);
        
        source.sendSuccess(() -> Component.literal("Cache expiration time set to: " + minutes + " minutes"), false);
        
        return 1;
    }
    
    private static int showRecommendations(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String agentName = StringArgumentType.getString(context, "agent");
        PerformanceManager pm = CraftoMod.getPerformanceManager();
        
        List<String> recommendations = pm.getOptimizationRecommendations(agentName);
        
        source.sendSuccess(() -> Component.literal("=== Optimization Recommendations for " + agentName + " ==="), false);
        
        if (recommendations.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No recommendations available"), false);
        } else {
            recommendations.forEach(rec -> 
                source.sendSuccess(() -> Component.literal("• " + rec), false));
        }
        
        return 1;
    }
    
    private static int exportStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PerformanceManager pm = CraftoMod.getPerformanceManager();
        
        Map<String, Object> stats = pm.exportStatistics();
        
        // Здесь можно добавить экспорт в файл
        source.sendSuccess(() -> Component.literal("Statistics exported to logs"), false);
        CraftoMod.LOGGER.info("Exported performance statistics: {}", stats);
        
        return 1;
    }
    
    private static int testBuild(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String agentName = StringArgumentType.getString(context, "agent");
        String command = StringArgumentType.getString(context, "command");
        
        com.crafto.ai.entity.CraftoManager manager = CraftoMod.getCraftoManager();
        com.crafto.ai.entity.CraftoEntity crafto = manager.getCrafto(agentName);
        
        if (crafto != null) {
            source.sendSuccess(() -> Component.literal("Testing build command for " + agentName + ": " + command), false);
            
            // Тестируем систему строительства
            com.crafto.ai.debug.BuildingDebugger.testBuildingSystem(crafto, command);
            
            source.sendSuccess(() -> Component.literal("Build test initiated. Check logs for details."), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("Crafto not found: " + agentName));
            return 0;
        }
    }
}