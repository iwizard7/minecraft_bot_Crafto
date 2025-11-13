package com.crafto.ai.action;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.action.actions.*;
import com.crafto.ai.ai.ResponseParser;
import com.crafto.ai.ai.TaskPlanner;
import com.crafto.ai.config.CraftoConfig;
import com.crafto.ai.entity.CraftoEntity;

import java.util.LinkedList;
import java.util.Queue;

public class ActionExecutor {
    private final CraftoEntity crafto;
    private TaskPlanner taskPlanner;  // Lazy-initialized to avoid loading dependencies on entity creation
    private final Queue<Task> taskQueue;
    
    private BaseAction currentAction;
    private String currentGoal;
    private int ticksSinceLastAction;
    private BaseAction idleFollowAction;  // Follow player when idle

    public ActionExecutor(CraftoEntity crafto) {
        this.crafto = crafto;
        this.taskPlanner = null;  // Will be initialized when first needed
        this.taskQueue = new LinkedList<>();
        this.ticksSinceLastAction = 0;
        this.idleFollowAction = null;
    }
    
    private TaskPlanner getTaskPlanner() {
        if (taskPlanner == null) {
            CraftoMod.LOGGER.info("Initializing TaskPlanner for Steve '{}'", crafto.getCraftoName());
            taskPlanner = new TaskPlanner();
        }
        return taskPlanner;
    }

    public void processNaturalLanguageCommand(String command) {
        CraftoMod.LOGGER.info("Crafto '{}' processing command: {}", crafto.getCraftoName(), command);
        
        // Специальные команды для отладки
        if (command.toLowerCase().contains("clear inventory") || command.toLowerCase().contains("reset inventory")) {
            com.crafto.ai.inventory.InventoryManager.clearInventory(crafto);
            sendToGUI(crafto.getCraftoName(), "Inventory cleared!");
            return;
        }
        
        if (command.toLowerCase().contains("show inventory") || command.toLowerCase().contains("inventory stats")) {
            String stats = com.crafto.ai.inventory.InventoryManager.getInventoryStats(crafto);
            sendToGUI(crafto.getCraftoName(), stats);
            return;
        }
        
        if (currentAction != null) {
            currentAction.cancel();
            currentAction = null;
        }
        
        if (idleFollowAction != null) {
            idleFollowAction.cancel();
            idleFollowAction = null;
        }
        
        try {
            ResponseParser.ParsedResponse response = null;
            
            // Проверяем, является ли это командой строительства
            if (com.crafto.ai.debug.BuildingDebugger.isBuildCommand(command)) {
                CraftoMod.LOGGER.info("Detected build command, using debug system");
                response = com.crafto.ai.debug.BuildingDebugger.createBuildResponse(command);
            } else {
                // Try cache first for performance
                // Кэширование команд отключено для упрощения
                response = null;
                
                if (response == null) {
                    // Cache miss - use AI
                    response = getTaskPlanner().planTasks(crafto, command);
                    if (response != null) {
                        // Кэширование команд отключено для упрощения
                    }
                } else {
                    CraftoMod.LOGGER.info("Using cached response for command: {}", command);
                }
            }
            
            if (response == null) {
                sendToGUI(crafto.getCraftoName(), "I couldn't understand that command.");
                return;
            }

            currentGoal = response.getPlan();
            crafto.getMemory().setCurrentGoal(currentGoal);
            
            taskQueue.clear();
            taskQueue.addAll(response.getTasks());
            
            // Send response to GUI pane only
            if (CraftoConfig.ENABLE_CHAT_RESPONSES.get()) {
                sendToGUI(crafto.getCraftoName(), "Okay! " + currentGoal);
            }
        } catch (NoClassDefFoundError e) {
            CraftoMod.LOGGER.error("Failed to initialize AI components", e);
            sendToGUI(crafto.getCraftoName(), "Sorry, I'm having trouble with my AI systems!");
        }
        
        CraftoMod.LOGGER.info("Crafto '{}' queued {} tasks", crafto.getCraftoName(), taskQueue.size());
    }
    
    /**
     * Send a message to the GUI pane (client-side only, no chat spam)
     */
    private void sendToGUI(String craftoName, String message) {
        if (crafto.level().isClientSide) {
            com.crafto.ai.client.CraftoGUI.addSteveMessage(craftoName, message);
        }
    }

    public void tick() {
        ticksSinceLastAction++;
        
        // PRIORITY: Check for player defense needs (every 10 ticks = 0.5 seconds)
        if (ticksSinceLastAction % 10 == 0) {
            checkPlayerDefense();
        }
        
        // Optimize: Only process every few ticks when idle to reduce CPU load
        if (currentAction == null && taskQueue.isEmpty() && currentGoal == null) {
            if (ticksSinceLastAction % 40 != 0) { // Only check every 2 seconds when idle
                return;
            }
        }
        
        if (currentAction != null) {
            if (currentAction.isComplete()) {
                ActionResult result = currentAction.getResult();
                CraftoMod.LOGGER.info("Crafto '{}' - Action completed: {} (Success: {})", 
                    crafto.getCraftoName(), result.getMessage(), result.isSuccess());
                
                crafto.getMemory().addAction(currentAction.getDescription());
                
                if (!result.isSuccess() && result.requiresReplanning()) {
                    // Action failed, need to replan
                    if (CraftoConfig.ENABLE_CHAT_RESPONSES.get()) {
                        sendToGUI(crafto.getCraftoName(), "Problem: " + result.getMessage());
                    }
                }
                
                currentAction = null;
            } else {
                // Reduce logging frequency for performance
                if (ticksSinceLastAction % 200 == 0) { // Every 10 seconds instead of 5
                    CraftoMod.LOGGER.info("Crafto '{}' - Ticking action: {}", 
                        crafto.getCraftoName(), currentAction.getDescription());
                }
                currentAction.tick();
                return;
            }
        }

        if (ticksSinceLastAction >= CraftoConfig.ACTION_TICK_DELAY.get()) {
            if (!taskQueue.isEmpty()) {
                Task nextTask = taskQueue.poll();
                executeTask(nextTask);
                ticksSinceLastAction = 0;
                return;
            }
        }
        
        // When completely idle (no tasks, no goal), follow nearest player
        if (taskQueue.isEmpty() && currentAction == null && currentGoal == null) {
            if (idleFollowAction == null) {
                idleFollowAction = new IdleFollowAction(crafto);
                idleFollowAction.start();
            } else if (idleFollowAction.isComplete()) {
                // Restart idle following if it stopped
                idleFollowAction = new IdleFollowAction(crafto);
                idleFollowAction.start();
            } else {
                // Continue idle following
                idleFollowAction.tick();
            }
        } else if (idleFollowAction != null) {
            idleFollowAction.cancel();
            idleFollowAction = null;
        }
        
        // Periodic cache cleanup (every 5 minutes)
        if (ticksSinceLastAction % 6000 == 0) {
            // Очистка кэша команд отключена для упрощения
        }
    }

    private void executeTask(Task task) {
        CraftoMod.LOGGER.info("Crafto '{}' executing task: {} (action type: {})", 
            crafto.getCraftoName(), task, task.getAction());
        
        currentAction = createAction(task);
        
        if (currentAction == null) {
            CraftoMod.LOGGER.error("FAILED to create action for task: {}", task);
            return;
        }

        CraftoMod.LOGGER.info("Created action: {} - starting now...", currentAction.getClass().getSimpleName());
        currentAction.start();
        CraftoMod.LOGGER.info("Action started! Is complete: {}", currentAction.isComplete());
    }

    private BaseAction createAction(Task task) {
        return switch (task.getAction()) {
            case "pathfind" -> new PathfindAction(crafto, task);
            case "mine" -> new MineBlockAction(crafto, task);
            case "place" -> new PlaceBlockAction(crafto, task);
            case "craft" -> new CraftItemAction(crafto, task);
            case "attack" -> new CombatAction(crafto, task);
            case "kill" -> new KillMobsAction(crafto, task);
            case "spawn" -> new SpawnMobsAction(crafto, task);
            case "follow" -> new FollowPlayerAction(crafto, task);
            case "gather" -> new GatherResourceAction(crafto, task);
            case "build" -> new BuildStructureAction(crafto, task);
            case "defend_player" -> new PlayerDefenseAction(crafto, task);
            default -> {
                CraftoMod.LOGGER.warn("Unknown action type: {}", task.getAction());
                yield null;
            }
        };
    }
    
    /**
     * Проверяет, нужна ли защита игрока, и автоматически начинает защиту при необходимости
     */
    private void checkPlayerDefense() {
        // Не прерываем важные действия для защиты
        if (currentAction instanceof BuildStructureAction || 
            currentAction instanceof PlayerDefenseAction ||
            currentAction instanceof CraftItemAction) {
            return;
        }
        
        // Найти ближайшего игрока
        net.minecraft.world.entity.player.Player nearestPlayer = findNearestPlayer();
        if (nearestPlayer == null) {
            return;
        }
        
        // Проверить, атакуют ли игрока
        if (PlayerDefenseAction.isPlayerUnderAttack(crafto, nearestPlayer)) {
            // Прерываем текущее действие для защиты игрока
            if (currentAction != null) {
                CraftoMod.LOGGER.info("Crafto '{}' interrupting current action to defend player {}", 
                    crafto.getCraftoName(), nearestPlayer.getName().getString());
                currentAction.cancel();
                currentAction = null;
            }
            
            // Очищаем очередь задач - защита игрока приоритетнее
            if (!taskQueue.isEmpty()) {
                CraftoMod.LOGGER.info("Crafto '{}' clearing task queue to defend player", crafto.getCraftoName());
                taskQueue.clear();
            }
            
            // Начинаем защиту
            currentAction = new PlayerDefenseAction(crafto, nearestPlayer);
            currentAction.start();
            
            CraftoMod.LOGGER.info("Crafto '{}' automatically started defending player {}", 
                crafto.getCraftoName(), nearestPlayer.getName().getString());
        }
    }
    
    /**
     * Найти ближайшего игрока в радиусе защиты
     */
    private net.minecraft.world.entity.player.Player findNearestPlayer() {
        net.minecraft.world.phys.AABB searchBox = crafto.getBoundingBox().inflate(20.0); // 20 блоков радиус
        java.util.List<net.minecraft.world.entity.Entity> entities = crafto.level().getEntities(crafto, searchBox);
        
        net.minecraft.world.entity.player.Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (net.minecraft.world.entity.Entity entity : entities) {
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                double distance = crafto.distanceTo(player);
                if (distance < nearestDistance) {
                    nearest = player;
                    nearestDistance = distance;
                }
            }
        }
        
        return nearest;
    }

    public void stopCurrentAction() {
        if (currentAction != null) {
            currentAction.cancel();
            currentAction = null;
        }
        if (idleFollowAction != null) {
            idleFollowAction.cancel();
            idleFollowAction = null;
        }
        taskQueue.clear();
        currentGoal = null;
    }

    public boolean isExecuting() {
        return currentAction != null || !taskQueue.isEmpty();
    }

    public String getCurrentGoal() {
        return currentGoal;
    }
}

