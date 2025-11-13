package com.crafto.ai.entity;

import com.crafto.ai.action.ActionExecutor;
import com.crafto.ai.memory.CraftoMemory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class CraftoEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> CRAFTO_NAME = 
        SynchedEntityData.defineId(CraftoEntity.class, EntityDataSerializers.STRING);

    private String craftoName;
    private CraftoMemory memory;
    private ActionExecutor actionExecutor;
    private int tickCounter = 0;
    private boolean isFlying = false;
    private boolean isInvulnerable = false;

    public CraftoEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.craftoName = "Crafto";
        this.memory = new CraftoMemory(this);
        this.actionExecutor = new ActionExecutor(this);
        this.setCustomNameVisible(true);
        
        this.isInvulnerable = true;
        this.setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            .add(Attributes.ATTACK_DAMAGE, 8.0D)
            .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CRAFTO_NAME, "Crafto");
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide) {
            actionExecutor.tick();
            
            // Periodic performance monitoring
            if (tickCounter % 600 == 0) { // Every 30 seconds
                com.crafto.ai.util.PerformanceMonitor.checkMemoryUsage();
            }
        }
        
        tickCounter++;
    }

    public void setCraftoName(String name) {
        this.craftoName = name;
        this.entityData.set(CRAFTO_NAME, name);
        this.setCustomName(Component.literal(name));
    }

    public String getCraftoName() {
        return this.craftoName;
    }

    public CraftoMemory getMemory() {
        return this.memory;
    }

    public ActionExecutor getActionExecutor() {
        return this.actionExecutor;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CraftoName", this.craftoName);
        
        CompoundTag memoryTag = new CompoundTag();
        this.memory.saveToNBT(memoryTag);
        tag.put("Memory", memoryTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CraftoName")) {
            this.setCraftoName(tag.getString("CraftoName"));
        }
        
        if (tag.contains("Memory")) {
            this.memory.loadFromNBT(tag.getCompound("Memory"));
        }
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                       MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                       @Nullable CompoundTag tag) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        return spawnData;
    }

    public void sendChatMessage(String message) {
        if (this.level().isClientSide) return;
        
        Component chatComponent = Component.literal("<" + this.craftoName + "> " + message);
        this.level().players().forEach(player -> player.sendSystemMessage(chatComponent));
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
    }

    public void setFlying(boolean flying) {
        this.isFlying = flying;
        this.setNoGravity(flying);
        this.setInvulnerableBuilding(flying);
    }

    public boolean isFlying() {
        return this.isFlying;
    }

    /**
     * Set invulnerability for building (immune to ALL damage: fire, lava, suffocation, fall, etc.)
     */
    public void setInvulnerableBuilding(boolean invulnerable) {
        this.isInvulnerable = invulnerable;
        this.setInvulnerable(invulnerable); // Minecraft's built-in invulnerability
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        if (this.isFlying && !this.level().isClientSide) {
            double motionY = this.getDeltaMovement().y;
            
            if (this.getNavigation().isInProgress()) {
                super.travel(travelVector);
                
                // But add ability to move vertically freely
                if (Math.abs(motionY) < 0.1) {
                    // Small upward force to prevent falling
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.05, 0));
                }
            } else {
                super.travel(travelVector);
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, net.minecraft.world.damagesource.DamageSource source) {
        // No fall damage when flying
        if (this.isFlying) {
            return false;
        }
        return super.causeFallDamage(distance, damageMultiplier, source);
    }
}

