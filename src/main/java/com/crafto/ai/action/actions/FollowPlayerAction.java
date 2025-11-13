package com.crafto.ai.action.actions;

import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class FollowPlayerAction extends BaseAction {
    private String playerName;
    private Player targetPlayer;
    private int ticksRunning;
    private static final int MAX_TICKS = 6000; // 5 minutes

    public FollowPlayerAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
    }

    @Override
    protected void onStart() {
        playerName = task.getStringParameter("player");
        ticksRunning = 0;
        
        findPlayer();
        
        if (targetPlayer == null) {
            result = ActionResult.failure("Player not found: " + playerName);
        }
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        
        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.success("Stopped following");
            return;
        }
        
        if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.isRemoved()) {
            findPlayer();
            if (targetPlayer == null) {
                result = ActionResult.failure("Lost track of player");
                return;
            }
        }
        
        double distance = crafto.distanceTo(targetPlayer);
        if (distance > 3.0) {
            crafto.getNavigation().moveTo(targetPlayer, 1.0);
        } else if (distance < 2.0) {
            crafto.getNavigation().stop();
        }
    }

    @Override
    protected void onCancel() {
        crafto.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Follow player " + playerName;
    }

    private void findPlayer() {
        java.util.List<? extends Player> players = crafto.level().players();
        
        // First try exact name match
        for (Player player : players) {
            if (player.getName().getString().equalsIgnoreCase(playerName)) {
                targetPlayer = player;
                return;
            }
        }
        
        if (playerName != null && (playerName.contains("PLAYER") || playerName.contains("NAME") || 
            playerName.equalsIgnoreCase("me") || playerName.equalsIgnoreCase("you") || playerName.isEmpty())) {
            Player nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            
            for (Player player : players) {
                double distance = crafto.distanceTo(player);
                if (distance < nearestDistance) {
                    nearest = player;
                    nearestDistance = distance;
                }
            }
            
            if (nearest != null) {
                targetPlayer = nearest;
                playerName = nearest.getName().getString(); // Update to actual name
                com.crafto.ai.CraftoMod.LOGGER.info("Crafto '{}' following nearest player: {}", 
                    crafto.getCraftoName(), playerName);
            }
        }
    }
}

