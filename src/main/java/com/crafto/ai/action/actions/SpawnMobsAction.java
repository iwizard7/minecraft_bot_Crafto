package com.crafto.ai.action.actions;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;

public class SpawnMobsAction extends BaseAction {
    private int mobCount;
    private int spawnedCount;
    private int ticksRunning;

    public SpawnMobsAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
    }

    @Override
    protected void onStart() {
        mobCount = task.getIntParameter("count", 5);
        spawnedCount = 0;
        ticksRunning = 0;
        
        CraftoMod.LOGGER.info("Crafto '{}' spawning {} test mobs for hunting", 
            crafto.getCraftoName(), mobCount);
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        
        if (spawnedCount >= mobCount) {
            result = ActionResult.success("Spawned " + spawnedCount + " test mobs");
            return;
        }
        
        // Spawn one mob every 10 ticks
        if (ticksRunning % 10 == 0 && spawnedCount < mobCount) {
            spawnTestMob();
        }
    }

    @Override
    protected void onCancel() {
        CraftoMod.LOGGER.info("Crafto '{}' mob spawning cancelled. Spawned {}/{} mobs", 
            crafto.getCraftoName(), spawnedCount, mobCount);
    }

    @Override
    public String getDescription() {
        return "Spawn " + mobCount + " test mobs (" + spawnedCount + "/" + mobCount + ")";
    }

    private void spawnTestMob() {
        if (!(crafto.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // Spawn zombie near Steve
        double angle = crafto.getRandom().nextDouble() * 2 * Math.PI;
        double distance = 5 + crafto.getRandom().nextDouble() * 10; // 5-15 blocks away
        
        double spawnX = crafto.getX() + Math.cos(angle) * distance;
        double spawnZ = crafto.getZ() + Math.sin(angle) * distance;
        BlockPos spawnPos = new BlockPos((int)spawnX, (int)crafto.getY(), (int)spawnZ);
        
        // Find safe spawn position
        BlockPos safePos = serverLevel.getHeightmapPos(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawnPos);
        
        Zombie zombie = EntityType.ZOMBIE.create(serverLevel);
        if (zombie != null) {
            zombie.moveTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
            zombie.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(safePos), 
                MobSpawnType.COMMAND, null, null);
            
            if (serverLevel.addFreshEntity(zombie)) {
                spawnedCount++;
                CraftoMod.LOGGER.info("Crafto '{}' spawned test zombie #{} at {}", 
                    crafto.getCraftoName(), spawnedCount, safePos);
            }
        }
    }
}