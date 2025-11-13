package com.crafto.ai.action.actions;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class KillMobsAction extends BaseAction {
    private String targetType;
    private int targetCount;
    private int killedCount;
    private LivingEntity currentTarget;
    private int ticksRunning;
    private int ticksStuck;
    private int ticksSinceLastKill;
    private double lastX, lastZ;
    
    private static final int MAX_TICKS = 12000; // 10 minutes max
    private static final int MAX_TICKS_WITHOUT_KILL = 1200; // 1 minute without kill = give up
    private static final double ATTACK_RANGE = 3.5;
    private static final double SEARCH_RANGE = 32.0;

    public KillMobsAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
    }

    @Override
    protected void onStart() {
        targetType = task.getStringParameter("target", "hostile");
        targetCount = task.getIntParameter("count", 10);
        killedCount = 0;
        ticksRunning = 0;
        ticksStuck = 0;
        ticksSinceLastKill = 0;
        
        // Make sure we're not flying
        crafto.setFlying(false);
        crafto.setInvulnerableBuilding(true);
        
        CraftoMod.LOGGER.info("Crafto '{}' starting mob hunt: kill {} {} mobs", 
            crafto.getCraftoName(), targetCount, targetType);
        
        findTarget();
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        ticksSinceLastKill++;
        
        // Check timeout conditions
        if (ticksRunning > MAX_TICKS) {
            finishHunt("Hunt timeout - time limit reached");
            return;
        }
        
        if (ticksSinceLastKill > MAX_TICKS_WITHOUT_KILL) {
            finishHunt("Hunt timeout - no kills for too long");
            return;
        }
        
        // Check if we've killed enough mobs
        if (killedCount >= targetCount) {
            finishHunt("Hunt complete - killed " + killedCount + "/" + targetCount + " mobs!");
            return;
        }
        
        // Check if current target died (count kill)
        if (currentTarget != null && (!currentTarget.isAlive() || currentTarget.isRemoved())) {
            if (!currentTarget.isAlive()) {
                // Target died, count it as a kill
                killedCount++;
                ticksSinceLastKill = 0;
                CraftoMod.LOGGER.info("Crafto '{}' killed mob! Progress: {}/{}", 
                    crafto.getCraftoName(), killedCount, targetCount);
            }
            currentTarget = null; // Clear dead target
        }
        
        // Find new target if we don't have one
        if (currentTarget == null) {
            // Search for new target more frequently (every 10 ticks = 0.5 seconds)
            if (ticksRunning % 10 == 0) {
                findTarget();
            }
            
            if (currentTarget == null) {
                // No target found, wander around to find mobs
                wanderToFindMobs();
                return;
            }
        }
        
        // Combat logic
        double distance = crafto.distanceTo(currentTarget);
        
        // Move towards target
        crafto.setSprinting(true);
        crafto.getNavigation().moveTo(currentTarget, 2.5);
        
        // If target is too far, teleport closer immediately
        if (distance > 15.0) {
            double dx = currentTarget.getX() - crafto.getX();
            double dz = currentTarget.getZ() - crafto.getZ();
            double dist = Math.sqrt(dx*dx + dz*dz);
            double moveAmount = dist - 8.0; // Get within 8 blocks
            
            crafto.teleportTo(
                crafto.getX() + (dx/dist) * moveAmount,
                currentTarget.getY(),
                crafto.getZ() + (dz/dist) * moveAmount
            );
            CraftoMod.LOGGER.info("Crafto '{}' teleported closer to distant target (was {}m away)", 
                crafto.getCraftoName(), (int)distance);
        }
        
        // Handle getting stuck
        handleStuckDetection(distance);
        
        // Attack if in range
        if (distance <= ATTACK_RANGE) {
            crafto.getLookControl().setLookAt(currentTarget);
            
            // Multiple attack methods for maximum effectiveness
            boolean attacked = crafto.doHurtTarget(currentTarget);
            crafto.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            
            // Alternative damage methods for guaranteed kills
            if (!attacked || ticksRunning % 2 == 0) {
                // Direct damage application with higher damage
                currentTarget.hurt(crafto.damageSources().mobAttack(crafto), 10.0f);
            }
            
            // Additional attacks for faster kills - attack every tick when in range
            crafto.doHurtTarget(currentTarget);
            
            // Extra damage for tough mobs
            if (currentTarget.getHealth() > 15.0f) {
                currentTarget.hurt(crafto.damageSources().mobAttack(crafto), 8.0f);
            }
            
            // Instant kill for low health mobs
            if (currentTarget.getHealth() <= 5.0f) {
                currentTarget.hurt(crafto.damageSources().mobAttack(crafto), 20.0f);
            }
            
            if (ticksRunning % 20 == 0) { // Every second
                CraftoMod.LOGGER.info("Crafto '{}' attacking {} at distance {} (health: {}/{})", 
                    crafto.getCraftoName(), currentTarget.getType().toString(), 
                    (int)distance, (int)currentTarget.getHealth(), (int)currentTarget.getMaxHealth());
            }
        }
        
        // Progress update every 5 seconds
        if (ticksRunning % 100 == 0) {
            CraftoMod.LOGGER.info("Crafto '{}' mob hunt progress: {}/{} killed", 
                crafto.getCraftoName(), killedCount, targetCount);
        }
    }

    @Override
    protected void onCancel() {
        crafto.setInvulnerableBuilding(false);
        crafto.getNavigation().stop();
        crafto.setSprinting(false);
        crafto.setFlying(false);
        currentTarget = null;
        CraftoMod.LOGGER.info("Crafto '{}' mob hunt cancelled. Killed {}/{} mobs", 
            crafto.getCraftoName(), killedCount, targetCount);
    }

    @Override
    public String getDescription() {
        return "Kill " + targetCount + " " + targetType + " mobs (" + killedCount + "/" + targetCount + ")";
    }

    private void findTarget() {
        AABB searchBox = crafto.getBoundingBox().inflate(SEARCH_RANGE);
        List<Entity> entities = crafto.level().getEntities(crafto, searchBox);
        
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int validTargetsFound = 0;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && isValidTarget(living)) {
                validTargetsFound++;
                double distance = crafto.distanceTo(living);
                if (distance < nearestDistance) {
                    nearest = living;
                    nearestDistance = distance;
                }
            }
        }
        
        currentTarget = nearest;
        if (currentTarget != null) {
            CraftoMod.LOGGER.info("Crafto '{}' targeting: {} at {}m (found {} valid targets)", 
                crafto.getCraftoName(), currentTarget.getType().toString(), (int)nearestDistance, validTargetsFound);
        } else if (ticksRunning % 100 == 0) { // Log every 5 seconds when no targets
            CraftoMod.LOGGER.info("Crafto '{}' found {} entities, {} valid targets in {}m radius", 
                crafto.getCraftoName(), entities.size(), validTargetsFound, (int)SEARCH_RANGE);
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
        String entityTypeName = entity.getType().toString().toLowerCase();
        
        // Debug logging for target matching - use INFO level to see in logs
        if (ticksRunning % 200 == 0) { // Every 10 seconds
            CraftoMod.LOGGER.info("Crafto '{}' checking entity: {} (type: {}) against target: {} (isMonster: {})", 
                crafto.getCraftoName(), entity.getName().getString(), entityTypeName, targetLower, 
                entity instanceof Monster);
        }
        
        // Match ANY hostile mob
        if (targetLower.contains("mob") || targetLower.contains("hostile") || 
            targetLower.contains("monster") || targetLower.equals("any") || targetLower.contains("мобов")) {
            
            // Primary check: Monster class
            boolean isMonster = entity instanceof Monster;
            
            // Secondary check: Common hostile mob types by name
            boolean isHostileByName = entityTypeName.contains("zombie") || 
                                    entityTypeName.contains("skeleton") || 
                                    entityTypeName.contains("spider") || 
                                    entityTypeName.contains("creeper") || 
                                    entityTypeName.contains("enderman") || 
                                    entityTypeName.contains("witch") || 
                                    entityTypeName.contains("slime") ||
                                    entityTypeName.contains("phantom") ||
                                    entityTypeName.contains("blaze") ||
                                    entityTypeName.contains("ghast") ||
                                    entityTypeName.contains("pillager") ||
                                    entityTypeName.contains("vindicator") ||
                                    entityTypeName.contains("evoker") ||
                                    entityTypeName.contains("ravager") ||
                                    entityTypeName.contains("vex") ||
                                    entityTypeName.contains("husk") ||
                                    entityTypeName.contains("stray") ||
                                    entityTypeName.contains("drowned");
            
            // Tertiary check: Mob category (aggressive mobs)
            boolean isAggressive = false;
            try {
                // Check if mob is naturally aggressive or has a target
                if (entity instanceof net.minecraft.world.entity.Mob mob) {
                    isAggressive = mob.getTarget() != null || mob.isAggressive();
                }
            } catch (Exception e) {
                // Ignore reflection errors
            }
            
            // Quaternary check: Health-based detection (most hostile mobs have specific health ranges)
            boolean hasHostileHealth = entity.getMaxHealth() >= 16.0f && entity.getMaxHealth() <= 100.0f;
            
            boolean isValidTarget = isMonster || isHostileByName || isAggressive || hasHostileHealth;
            
            if (ticksRunning % 200 == 0) { // Log every 10 seconds for debugging
                CraftoMod.LOGGER.info("Crafto '{}' checking hostile mob: {} (type: {}) isMonster: {} isHostileByName: {} isAggressive: {} hasHostileHealth: {} -> VALID: {}", 
                    crafto.getCraftoName(), entity.getName().getString(), entityTypeName, 
                    isMonster, isHostileByName, isAggressive, hasHostileHealth, isValidTarget);
            }
            
            return isValidTarget;
        }
        
        // Match specific entity types
        // Common mob name mappings
        if (targetLower.contains("zombie") || targetLower.contains("зомби")) {
            return entityTypeName.contains("zombie") || entityTypeName.contains("husk") || entityTypeName.contains("drowned");
        }
        if (targetLower.contains("skeleton") || targetLower.contains("скелет")) {
            return entityTypeName.contains("skeleton") || entityTypeName.contains("stray");
        }
        if (targetLower.contains("spider") || targetLower.contains("паук")) {
            return entityTypeName.contains("spider");
        }
        if (targetLower.contains("creeper") || targetLower.contains("крипер")) {
            return entityTypeName.contains("creeper");
        }
        if (targetLower.contains("enderman") || targetLower.contains("эндермен")) {
            return entityTypeName.contains("enderman");
        }
        if (targetLower.contains("witch") || targetLower.contains("ведьм")) {
            return entityTypeName.contains("witch");
        }
        if (targetLower.contains("slime") || targetLower.contains("слизн")) {
            return entityTypeName.contains("slime");
        }
        if (targetLower.contains("pillager") || targetLower.contains("пиллагер")) {
            return entityTypeName.contains("pillager") || entityTypeName.contains("vindicator") || 
                   entityTypeName.contains("evoker") || entityTypeName.contains("ravager");
        }
        
        return entityTypeName.contains(targetLower);
    }

    private void handleStuckDetection(double distance) {
        double currentX = crafto.getX();
        double currentZ = crafto.getZ();
        
        if (Math.abs(currentX - lastX) < 0.1 && Math.abs(currentZ - lastZ) < 0.1) {
            ticksStuck++;
            
            if (ticksStuck > 40 && distance > ATTACK_RANGE) {
                // Teleport closer to target if stuck
                double dx = currentTarget.getX() - crafto.getX();
                double dz = currentTarget.getZ() - crafto.getZ();
                double dist = Math.sqrt(dx*dx + dz*dz);
                double moveAmount = Math.min(4.0, dist - ATTACK_RANGE);
                
                crafto.teleportTo(
                    crafto.getX() + (dx/dist) * moveAmount,
                    crafto.getY(),
                    crafto.getZ() + (dz/dist) * moveAmount
                );
                ticksStuck = 0;
                CraftoMod.LOGGER.info("Crafto '{}' was stuck, teleported closer to target", 
                    crafto.getCraftoName());
            }
        } else {
            ticksStuck = 0;
        }
        
        lastX = currentX;
        lastZ = currentZ;
    }

    private void wanderToFindMobs() {
        // If no navigation is active, pick a random direction to explore
        if (!crafto.getNavigation().isInProgress()) {
            double angle = crafto.getRandom().nextDouble() * 2 * Math.PI;
            double distance = 10 + crafto.getRandom().nextDouble() * 20; // 10-30 blocks
            
            double targetX = crafto.getX() + Math.cos(angle) * distance;
            double targetZ = crafto.getZ() + Math.sin(angle) * distance;
            net.minecraft.core.BlockPos heightPos = new net.minecraft.core.BlockPos((int)targetX, 0, (int)targetZ);
            double targetY = crafto.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 
                heightPos).getY();
            
            crafto.getNavigation().moveTo(targetX, targetY, targetZ, 1.5);
            
            if (ticksRunning % 100 == 0) { // Every 5 seconds
                CraftoMod.LOGGER.info("Crafto '{}' wandering to find mobs... ({}/{} killed)", 
                    crafto.getCraftoName(), killedCount, targetCount);
            }
        }
    }

    private void finishHunt(String reason) {
        crafto.setInvulnerableBuilding(false);
        crafto.setSprinting(false);
        crafto.getNavigation().stop();
        
        CraftoMod.LOGGER.info("Crafto '{}' finished mob hunt: {} - Final score: {}/{}", 
            crafto.getCraftoName(), reason, killedCount, targetCount);
        
        if (killedCount >= targetCount) {
            result = ActionResult.success("Successfully killed " + killedCount + "/" + targetCount + " " + targetType + " mobs!");
        } else {
            result = ActionResult.success("Killed " + killedCount + "/" + targetCount + " " + targetType + " mobs - " + reason);
        }
    }
}