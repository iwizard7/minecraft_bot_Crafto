package com.crafto.ai.action.actions;

import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class CombatAction extends BaseAction {
    private String targetType;
    private LivingEntity target;
    private int ticksRunning;
    private int ticksStuck;
    private double lastX, lastZ;
    private static final int MAX_TICKS = 600;
    private static final double ATTACK_RANGE = 3.5;

    public CombatAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
    }

    @Override
    protected void onStart() {
        targetType = task.getStringParameter("target");
        ticksRunning = 0;
        ticksStuck = 0;
        
        // Make sure we're not flying (in case we were building)
        crafto.setFlying(false);
        
        crafto.setInvulnerableBuilding(true);
        
        findTarget();
        
        if (target == null) {
            com.crafto.ai.CraftoMod.LOGGER.warn("Crafto '{}' no targets nearby", crafto.getCraftoName());
        }
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        
        if (ticksRunning > MAX_TICKS) {
            // Combat complete - clean up and disable invulnerability
            crafto.setInvulnerableBuilding(false);
            crafto.setSprinting(false);
            crafto.getNavigation().stop();
            com.crafto.ai.CraftoMod.LOGGER.info("Crafto '{}' combat complete, invulnerability disabled", 
                crafto.getCraftoName());
            result = ActionResult.success("Combat complete");
            return;
        }
        
        // Re-search for targets periodically or if current target is invalid
        if (target == null || !target.isAlive() || target.isRemoved()) {
            if (ticksRunning % 20 == 0) {
                findTarget();
            }
            if (target == null) {
                return; // Keep searching
            }
        }
        
        double distance = crafto.distanceTo(target);
        
        crafto.setSprinting(true);
        crafto.getNavigation().moveTo(target, 2.5); // High speed multiplier for sprinting
        
        double currentX = crafto.getX();
        double currentZ = crafto.getZ();
        if (Math.abs(currentX - lastX) < 0.1 && Math.abs(currentZ - lastZ) < 0.1) {
            ticksStuck++;
            
            if (ticksStuck > 40 && distance > ATTACK_RANGE) {
                // Teleport 4 blocks closer to target
                double dx = target.getX() - crafto.getX();
                double dz = target.getZ() - crafto.getZ();
                double dist = Math.sqrt(dx*dx + dz*dz);
                double moveAmount = Math.min(4.0, dist - ATTACK_RANGE);
                
                crafto.teleportTo(
                    crafto.getX() + (dx/dist) * moveAmount,
                    crafto.getY(),
                    crafto.getZ() + (dz/dist) * moveAmount
                );
                ticksStuck = 0;
                com.crafto.ai.CraftoMod.LOGGER.info("Crafto '{}' was stuck, teleported closer to target", 
                    crafto.getCraftoName());
            }
        } else {
            ticksStuck = 0;
        }
        lastX = currentX;
        lastZ = currentZ;
        
        if (distance <= ATTACK_RANGE) {
            crafto.getLookControl().setLookAt(target);
            
            // Multiple attack methods for maximum effectiveness
            boolean attacked = crafto.doHurtTarget(target);
            crafto.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            
            // Alternative damage method if primary doesn't work
            if (!attacked || ticksRunning % 2 == 0) {
                target.hurt(crafto.damageSources().mobAttack(crafto), 8.0f);
            }
            
            // Additional attacks for faster kills
            if (ticksRunning % 5 == 0) {
                crafto.doHurtTarget(target);
            }
            
            // Extra damage for tough mobs
            if (target.getHealth() > 15.0f) {
                target.hurt(crafto.damageSources().mobAttack(crafto), 6.0f);
            }
        }
    }

    @Override
    protected void onCancel() {
        crafto.setInvulnerableBuilding(false);
        crafto.getNavigation().stop();
        crafto.setSprinting(false);
        crafto.setFlying(false);
        target = null;
        com.crafto.ai.CraftoMod.LOGGER.info("Crafto '{}' combat cancelled, invulnerability disabled", 
            crafto.getCraftoName());
    }

    @Override
    public String getDescription() {
        return "Attack " + targetType;
    }

    private void findTarget() {
        AABB searchBox = crafto.getBoundingBox().inflate(32.0);
        List<Entity> entities = crafto.level().getEntities(crafto, searchBox);
        
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && isValidTarget(living)) {
                double distance = crafto.distanceTo(living);
                if (distance < nearestDistance) {
                    nearest = living;
                    nearestDistance = distance;
                }
            }
        }
        
        target = nearest;
        if (target != null) {
            com.crafto.ai.CraftoMod.LOGGER.info("Crafto '{}' locked onto: {} at {}m", 
                crafto.getCraftoName(), target.getType().toString(), (int)nearestDistance);
        }
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (!entity.isAlive() || entity.isRemoved()) {
            return false;
        }
        
        // Don't attack other Steves or players
        if (entity instanceof CraftoEntity || entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }
        
        String targetLower = targetType.toLowerCase();
        
        // Match ANY hostile mob
        if (targetLower.contains("mob") || targetLower.contains("hostile") || 
            targetLower.contains("monster") || targetLower.equals("any")) {
            return entity instanceof Monster;
        }
        
        // Match specific entity type
        String entityTypeName = entity.getType().toString().toLowerCase();
        return entityTypeName.contains(targetLower);
    }
}
