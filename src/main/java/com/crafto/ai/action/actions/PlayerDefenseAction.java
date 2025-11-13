package com.crafto.ai.action.actions;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Автоматическая защита игрока от враждебных мобов
 */
public class PlayerDefenseAction extends BaseAction {
    private Player protectedPlayer;
    private LivingEntity currentThreat;
    private int ticksRunning;
    private int ticksWithoutThreat;
    private int ticksStuck;
    private double lastX, lastZ;
    
    private static final double PROTECTION_RANGE = 16.0; // Радиус защиты игрока
    private static final double ATTACK_RANGE = 3.5; // Радиус атаки
    private static final double THREAT_DETECTION_RANGE = 20.0; // Радиус обнаружения угроз
    private static final int MAX_TICKS_WITHOUT_THREAT = 200; // 10 секунд без угроз = завершить защиту
    
    public PlayerDefenseAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
    }
    
    public PlayerDefenseAction(CraftoEntity crafto, Player player) {
        super(crafto, new Task("defend_player", new java.util.HashMap<>()));
        this.protectedPlayer = player;
    }

    @Override
    protected void onStart() {
        ticksRunning = 0;
        ticksWithoutThreat = 0;
        ticksStuck = 0;
        
        // Найти ближайшего игрока если не указан
        if (protectedPlayer == null) {
            protectedPlayer = findNearestPlayer();
        }
        
        if (protectedPlayer == null) {
            result = ActionResult.failure("No player to protect found");
            return;
        }
        
        // Настройки для боя
        crafto.setFlying(false);
        crafto.setInvulnerableBuilding(true);
        crafto.setSprinting(true);
        
        CraftoMod.LOGGER.info("Crafto '{}' starting player defense for {}", 
            crafto.getCraftoName(), protectedPlayer.getName().getString());
        
        findThreat();
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        
        // Проверяем, жив ли защищаемый игрок
        if (protectedPlayer == null || !protectedPlayer.isAlive() || protectedPlayer.isRemoved()) {
            finishDefense("Protected player is no longer available");
            return;
        }
        
        // Проверяем расстояние до игрока
        double distanceToPlayer = crafto.distanceTo(protectedPlayer);
        if (distanceToPlayer > PROTECTION_RANGE * 2) {
            finishDefense("Player moved too far away");
            return;
        }
        
        // Проверяем текущую угрозу
        if (currentThreat != null && (!currentThreat.isAlive() || currentThreat.isRemoved())) {
            CraftoMod.LOGGER.info("Crafto '{}' eliminated threat: {}", 
                crafto.getCraftoName(), currentThreat.getType().toString());
            currentThreat = null;
            ticksWithoutThreat = 0;
        }
        
        // Ищем новые угрозы
        if (currentThreat == null) {
            findThreat();
            if (currentThreat == null) {
                ticksWithoutThreat++;
                
                // Если долго нет угроз, завершаем защиту
                if (ticksWithoutThreat > MAX_TICKS_WITHOUT_THREAT) {
                    finishDefense("No threats detected for extended period");
                    return;
                }
                
                // Патрулируем вокруг игрока
                patrolAroundPlayer();
                return;
            } else {
                ticksWithoutThreat = 0;
            }
        }
        
        // Боевая логика
        double distanceToThreat = crafto.distanceTo(currentThreat);
        
        // Движение к угрозе
        crafto.setSprinting(true);
        crafto.getNavigation().moveTo(currentThreat, 2.5);
        
        // Телепорт если угроза слишком далеко
        if (distanceToThreat > 15.0) {
            teleportCloserToThreat();
        }
        
        // Обработка застревания
        handleStuckDetection(distanceToThreat);
        
        // Атака если в радиусе
        if (distanceToThreat <= ATTACK_RANGE) {
            attackThreat();
        }
        
        // Логирование каждые 5 секунд
        if (ticksRunning % 100 == 0) {
            CraftoMod.LOGGER.info("Crafto '{}' defending {} from {} (distance: {}m)", 
                crafto.getCraftoName(), protectedPlayer.getName().getString(),
                currentThreat != null ? currentThreat.getType().toString() : "no threat",
                currentThreat != null ? (int)distanceToThreat : 0);
        }
    }

    @Override
    protected void onCancel() {
        crafto.setInvulnerableBuilding(false);
        crafto.setSprinting(false);
        crafto.getNavigation().stop();
        currentThreat = null;
        
        CraftoMod.LOGGER.info("Crafto '{}' player defense cancelled", crafto.getCraftoName());
    }

    @Override
    public String getDescription() {
        if (protectedPlayer != null) {
            return "Defending " + protectedPlayer.getName().getString() + 
                   (currentThreat != null ? " from " + currentThreat.getType().toString() : "");
        }
        return "Player defense";
    }
    
    private void findThreat() {
        if (protectedPlayer == null) return;
        
        AABB searchBox = protectedPlayer.getBoundingBox().inflate(THREAT_DETECTION_RANGE);
        List<Entity> entities = crafto.level().getEntities(crafto, searchBox);
        
        LivingEntity nearestThreat = null;
        double nearestDistance = Double.MAX_VALUE;
        int threatsFound = 0;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && isThreatToPlayer(living)) {
                threatsFound++;
                double distanceToPlayer = protectedPlayer.distanceTo(living);
                
                // Приоритет угрозам ближе к игроку
                if (distanceToPlayer < nearestDistance) {
                    nearestThreat = living;
                    nearestDistance = distanceToPlayer;
                }
            }
        }
        
        currentThreat = nearestThreat;
        
        if (currentThreat != null) {
            CraftoMod.LOGGER.info("Crafto '{}' detected threat: {} at {}m from player (found {} total threats)", 
                crafto.getCraftoName(), currentThreat.getType().toString(), 
                (int)nearestDistance, threatsFound);
        }
    }
    
    private boolean isThreatToPlayer(LivingEntity entity) {
        if (!entity.isAlive() || entity.isRemoved()) {
            return false;
        }
        
        // Не атакуем других Стивов или игроков
        if (entity instanceof CraftoEntity || entity instanceof Player) {
            return false;
        }
        
        // Основная проверка - враждебные мобы
        if (entity instanceof Monster) {
            return true;
        }
        
        // Дополнительная проверка - мобы, которые атакуют игрока
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            LivingEntity target = mob.getTarget();
            if (target instanceof Player) {
                return true;
            }
        }
        
        // Проверка по типу моба
        String entityTypeName = entity.getType().toString().toLowerCase();
        return entityTypeName.contains("zombie") || 
               entityTypeName.contains("skeleton") || 
               entityTypeName.contains("spider") || 
               entityTypeName.contains("creeper") || 
               entityTypeName.contains("enderman") || 
               entityTypeName.contains("witch") || 
               entityTypeName.contains("slime") ||
               entityTypeName.contains("phantom") ||
               entityTypeName.contains("blaze") ||
               entityTypeName.contains("ghast");
    }
    
    private void patrolAroundPlayer() {
        if (protectedPlayer == null) return;
        
        // Если не движемся, выбираем точку патрулирования вокруг игрока
        if (!crafto.getNavigation().isInProgress()) {
            double angle = crafto.getRandom().nextDouble() * 2 * Math.PI;
            double distance = 3 + crafto.getRandom().nextDouble() * 5; // 3-8 блоков от игрока
            
            double targetX = protectedPlayer.getX() + Math.cos(angle) * distance;
            double targetZ = protectedPlayer.getZ() + Math.sin(angle) * distance;
            double targetY = protectedPlayer.getY();
            
            crafto.getNavigation().moveTo(targetX, targetY, targetZ, 1.0);
        }
    }
    
    private void teleportCloserToThreat() {
        if (currentThreat == null) return;
        
        double dx = currentThreat.getX() - crafto.getX();
        double dz = currentThreat.getZ() - crafto.getZ();
        double dist = Math.sqrt(dx*dx + dz*dz);
        double moveAmount = dist - 8.0; // Подходим на 8 блоков
        
        crafto.teleportTo(
            crafto.getX() + (dx/dist) * moveAmount,
            currentThreat.getY(),
            crafto.getZ() + (dz/dist) * moveAmount
        );
        
        CraftoMod.LOGGER.info("Crafto '{}' teleported closer to threat (was {}m away)", 
            crafto.getCraftoName(), (int)dist);
    }
    
    private void handleStuckDetection(double distanceToThreat) {
        double currentX = crafto.getX();
        double currentZ = crafto.getZ();
        
        if (Math.abs(currentX - lastX) < 0.1 && Math.abs(currentZ - lastZ) < 0.1) {
            ticksStuck++;
            
            if (ticksStuck > 40 && distanceToThreat > ATTACK_RANGE) {
                teleportCloserToThreat();
                ticksStuck = 0;
            }
        } else {
            ticksStuck = 0;
        }
        
        lastX = currentX;
        lastZ = currentZ;
    }
    
    private void attackThreat() {
        if (currentThreat == null) return;
        
        crafto.getLookControl().setLookAt(currentThreat);
        
        // Множественные атаки для максимальной эффективности
        boolean attacked = crafto.doHurtTarget(currentThreat);
        crafto.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        
        // Альтернативный метод урона если основной не работает
        if (!attacked || ticksRunning % 3 == 0) {
            currentThreat.hurt(crafto.damageSources().mobAttack(crafto), 8.0f);
        }
        
        // Дополнительные атаки для быстрого убийства
        if (ticksRunning % 5 == 0) {
            crafto.doHurtTarget(currentThreat);
        }
    }
    
    private void finishDefense(String reason) {
        crafto.setInvulnerableBuilding(false);
        crafto.setSprinting(false);
        crafto.getNavigation().stop();
        
        CraftoMod.LOGGER.info("Crafto '{}' finished player defense: {}", 
            crafto.getCraftoName(), reason);
        
        result = ActionResult.success("Player defense completed - " + reason);
    }
    
    private Player findNearestPlayer() {
        AABB searchBox = crafto.getBoundingBox().inflate(PROTECTION_RANGE);
        List<Entity> entities = crafto.level().getEntities(crafto, searchBox);
        
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Entity entity : entities) {
            if (entity instanceof Player player) {
                double distance = crafto.distanceTo(player);
                if (distance < nearestDistance) {
                    nearest = player;
                    nearestDistance = distance;
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * Статический метод для проверки, нужна ли защита игрока
     */
    public static boolean isPlayerUnderAttack(CraftoEntity crafto, Player player) {
        if (player == null || !player.isAlive()) return false;
        
        AABB searchBox = player.getBoundingBox().inflate(THREAT_DETECTION_RANGE);
        List<Entity> entities = crafto.level().getEntities(crafto, searchBox);
        
        for (Entity entity : entities) {
            if (entity instanceof Monster monster) {
                // Проверяем, атакует ли монстр игрока
                if (monster.getTarget() == player) {
                    return true;
                }
                
                // Проверяем расстояние - если монстр близко к игроку, это угроза
                if (player.distanceTo(monster) < 8.0) {
                    return true;
                }
            }
        }
        
        return false;
    }
}