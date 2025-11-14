package com.crafto.ai.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.crafto.ai.CraftoMod;
import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.entity.CraftoManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class CraftoCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("crafto")
            .then(Commands.literal("spawn")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(CraftoCommands::spawnCrafto)))
            .then(Commands.literal("remove")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(CraftoCommands::removeCrafto)))
            .then(Commands.literal("list")
                .executes(CraftoCommands::listCraftos))
            .then(Commands.literal("stop")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(CraftoCommands::stopCrafto)))
            .then(Commands.literal("tell")
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(CraftoCommands::tellCrafto))))
        );
        
        // Регистрируем команду производительности
        PerformanceCommand.register(dispatcher);
        
        // Регистрируем команды исследования и навигации
        registerExplorationCommands(dispatcher);
    }

    private static int spawnCrafto(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();
        
        ServerLevel serverLevel = source.getLevel();
        if (serverLevel == null) {
            source.sendFailure(Component.literal("Command must be run on server"));
            return 0;
        }

        CraftoManager manager = CraftoMod.getCraftoManager();
        
        Vec3 sourcePos = source.getPosition();
        if (source.getEntity() != null) {
            Vec3 lookVec = source.getEntity().getLookAngle();
            sourcePos = sourcePos.add(lookVec.x * 3, 0, lookVec.z * 3);
        } else {
            sourcePos = sourcePos.add(3, 0, 0);
        }
        Vec3 spawnPos = sourcePos;
        
        CraftoEntity crafto = manager.spawnCrafto(serverLevel, spawnPos, name);
        if (crafto != null) {
            source.sendSuccess(() -> Component.literal("Spawned Crafto: " + name), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to spawn Crafto. Name may already exist or max limit reached."));
            return 0;
        }
    }

    private static int removeCrafto(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();
        
        CraftoManager manager = CraftoMod.getCraftoManager();
        if (manager.removeCrafto(name)) {
            source.sendSuccess(() -> Component.literal("Removed Crafto: " + name), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Crafto not found: " + name));
            return 0;
        }
    }

    private static int listCraftos(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CraftoManager manager = CraftoMod.getCraftoManager();
        
        var names = manager.getCraftoNames();
        if (names.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No active Craftos"), false);
        } else {
            source.sendSuccess(() -> Component.literal("Active Craftos (" + names.size() + "): " + String.join(", ", names)), false);
        }
        return 1;
    }

    private static int stopCrafto(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();
        
        CraftoManager manager = CraftoMod.getCraftoManager();
        CraftoEntity crafto = manager.getCrafto(name);
        
        if (crafto != null) {
            crafto.getActionExecutor().stopCurrentAction();
            crafto.getMemory().clearTaskQueue();
            source.sendSuccess(() -> Component.literal("Stopped Crafto: " + name), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Crafto not found: " + name));
            return 0;
        }
    }

    private static int tellCrafto(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        String command = StringArgumentType.getString(context, "command");
        CommandSourceStack source = context.getSource();
        
        CraftoManager manager = CraftoMod.getCraftoManager();
        CraftoEntity crafto = manager.getCrafto(name);
        
        if (crafto != null) {
            // Disabled command feedback message
            // source.sendSuccess(() -> Component.literal("Instructing " + name + ": " + command), true);
            
            new Thread(() -> {
                crafto.getActionExecutor().processNaturalLanguageCommand(command);
            }).start();
            
            return 1;
        } else {
            source.sendFailure(Component.literal("Crafto not found: " + name));
            return 0;
        }
    }
    
    private static void registerExplorationCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("crafto")
            .then(Commands.literal("explore")
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(16, 500))
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            int radius = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "radius");
                            return executeExplorationCommand(context, name, "explore", radius);
                        }))))
            .then(Commands.literal("waypoint")
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("waypoint_name", StringArgumentType.string())
                            .then(Commands.argument("type", StringArgumentType.string())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    String waypointName = StringArgumentType.getString(context, "waypoint_name");
                                    String type = StringArgumentType.getString(context, "type");
                                    return executeWaypointCommand(context, name, "create", waypointName, type);
                                })))))
                .then(Commands.literal("navigate")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("waypoint_name", StringArgumentType.string())
                            .executes(context -> {
                                String name = StringArgumentType.getString(context, "name");
                                String waypointName = StringArgumentType.getString(context, "waypoint_name");
                                return executeWaypointCommand(context, name, "navigate", waypointName, "");
                            })))))
            .then(Commands.literal("find")
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("resource", StringArgumentType.string())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            String resource = StringArgumentType.getString(context, "resource");
                            return executeFindCommand(context, name, resource);
                        }))))
            .then(Commands.literal("map")
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(50, 1000))
                            .executes(context -> {
                                String name = StringArgumentType.getString(context, "name");
                                int radius = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "radius");
                                return executeMapCommand(context, name, radius);
                            })))))
            .then(Commands.literal("stats")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(context -> {
                        String name = StringArgumentType.getString(context, "name");
                        return executeStatsCommand(context, name);
                    })))
        );
    }
    
    private static int executeExplorationCommand(CommandContext<CommandSourceStack> context, String craftoName, String action, int radius) {
        CommandSourceStack source = context.getSource();
        CraftoManager manager = CraftoMod.getCraftoManager();
        CraftoEntity crafto = manager.getCrafto(craftoName);
        
        if (crafto == null) {
            source.sendFailure(Component.literal("Crafto not found: " + craftoName));
            return 0;
        }
        
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            ExplorationCommands.exploreArea(player, crafto, radius);
            source.sendSuccess(() -> Component.literal("Started exploration with " + craftoName), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Command must be run by a player"));
            return 0;
        }
    }
    
    private static int executeWaypointCommand(CommandContext<CommandSourceStack> context, String craftoName, String action, String waypointName, String type) {
        CommandSourceStack source = context.getSource();
        CraftoManager manager = CraftoMod.getCraftoManager();
        CraftoEntity crafto = manager.getCrafto(craftoName);
        
        if (crafto == null) {
            source.sendFailure(Component.literal("Crafto not found: " + craftoName));
            return 0;
        }
        
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            if ("create".equals(action)) {
                ExplorationCommands.createWaypoint(player, crafto, waypointName, type);
            } else if ("navigate".equals(action)) {
                ExplorationCommands.navigateToWaypoint(player, crafto, waypointName);
            }
            source.sendSuccess(() -> Component.literal("Executed waypoint command with " + craftoName), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Command must be run by a player"));
            return 0;
        }
    }
    
    private static int executeFindCommand(CommandContext<CommandSourceStack> context, String craftoName, String resource) {
        CommandSourceStack source = context.getSource();
        CraftoManager manager = CraftoMod.getCraftoManager();
        CraftoEntity crafto = manager.getCrafto(craftoName);
        
        if (crafto == null) {
            source.sendFailure(Component.literal("Crafto not found: " + craftoName));
            return 0;
        }
        
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            ExplorationCommands.findResources(player, crafto, resource);
            source.sendSuccess(() -> Component.literal("Searching for " + resource + " with " + craftoName), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Command must be run by a player"));
            return 0;
        }
    }
    
    private static int executeMapCommand(CommandContext<CommandSourceStack> context, String craftoName, int radius) {
        CommandSourceStack source = context.getSource();
        CraftoManager manager = CraftoMod.getCraftoManager();
        CraftoEntity crafto = manager.getCrafto(craftoName);
        
        if (crafto == null) {
            source.sendFailure(Component.literal("Crafto not found: " + craftoName));
            return 0;
        }
        
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            ExplorationCommands.exportMap(player, crafto, "PlayerMap_" + System.currentTimeMillis(), radius);
            source.sendSuccess(() -> Component.literal("Creating map with " + craftoName), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Command must be run by a player"));
            return 0;
        }
    }
    
    private static int executeStatsCommand(CommandContext<CommandSourceStack> context, String craftoName) {
        CommandSourceStack source = context.getSource();
        CraftoManager manager = CraftoMod.getCraftoManager();
        CraftoEntity crafto = manager.getCrafto(craftoName);
        
        if (crafto == null) {
            source.sendFailure(Component.literal("Crafto not found: " + craftoName));
            return 0;
        }
        
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            ExplorationCommands.showExplorationStats(player, crafto);
            ExplorationCommands.showNavigationStats(player, crafto);
            source.sendSuccess(() -> Component.literal("Showing stats for " + craftoName), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Command must be run by a player"));
            return 0;
        }
    }
}

