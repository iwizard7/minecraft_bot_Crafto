package com.crafto.ai.entity;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.config.CraftoConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CraftoManager {
    private final Map<String, CraftoEntity> activeCraftos;
    private final Map<UUID, CraftoEntity> craftosByUUID;

    public CraftoManager() {
        this.activeCraftos = new ConcurrentHashMap<>();
        this.craftosByUUID = new ConcurrentHashMap<>();
    }

    public CraftoEntity spawnCrafto(ServerLevel level, Vec3 position, String name) {        CraftoMod.LOGGER.info("Current active Steves: {}", activeCraftos.size());
        
        if (activeCraftos.containsKey(name)) {
            CraftoMod.LOGGER.warn("Crafto name '{}' already exists", name);
            return null;
        }        int maxSteves = CraftoConfig.MAX_ACTIVE_CRAFTOS.get();        if (activeCraftos.size() >= maxSteves) {
            CraftoMod.LOGGER.warn("Max Steve limit reached: {}", maxSteves);
            return null;
        }        CraftoEntity crafto;
        try {            CraftoMod.LOGGER.info("EntityType: {}", CraftoMod.CRAFTO_ENTITY.get());
            crafto = new CraftoEntity(CraftoMod.CRAFTO_ENTITY.get(), level);        } catch (Throwable e) {
            CraftoMod.LOGGER.error("Failed to create Steve entity", e);
            CraftoMod.LOGGER.error("Exception class: {}", e.getClass().getName());
            CraftoMod.LOGGER.error("Exception message: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }

        try {            crafto.setCraftoName(name);            crafto.setPos(position.x, position.y, position.z);            boolean added = level.addFreshEntity(crafto);            if (added) {
                activeCraftos.put(name, crafto);
                craftosByUUID.put(crafto.getUUID(), crafto);
                CraftoMod.LOGGER.info("Successfully spawned Steve: {} with UUID {} at {}", name, crafto.getUUID(), position);                return crafto;
            } else {
                CraftoMod.LOGGER.error("Failed to add Steve entity to world (addFreshEntity returned false)");
                CraftoMod.LOGGER.error("=== SPAWN ATTEMPT FAILED ===");
            }
        } catch (Throwable e) {
            CraftoMod.LOGGER.error("Exception during spawn setup", e);
            CraftoMod.LOGGER.error("=== SPAWN ATTEMPT FAILED WITH EXCEPTION ===");
            e.printStackTrace();
        }

        return null;
    }

    public CraftoEntity getCrafto(String name) {
        return activeCraftos.get(name);
    }

    public CraftoEntity getCrafto(UUID uuid) {
        return craftosByUUID.get(uuid);
    }

    public boolean removeCrafto(String name) {
        CraftoEntity crafto = activeCraftos.remove(name);
        if (crafto != null) {
            craftosByUUID.remove(crafto.getUUID());
            crafto.discard();            return true;
        }
        return false;
    }

    public void clearAllCraftos() {
        CraftoMod.LOGGER.info("Clearing {} Steve entities", activeCraftos.size());
        for (CraftoEntity crafto : activeCraftos.values()) {
            crafto.discard();
        }
        activeCraftos.clear();
        craftosByUUID.clear();    }

    public Collection<CraftoEntity> getAllSteves() {
        return Collections.unmodifiableCollection(activeCraftos.values());
    }

    public List<String> getCraftoNames() {
        return new ArrayList<>(activeCraftos.keySet());
    }

    public int getActiveCount() {
        return activeCraftos.size();
    }

    public void tick(ServerLevel level) {
        // Clean up dead or removed Steves
        Iterator<Map.Entry<String, CraftoEntity>> iterator = activeCraftos.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CraftoEntity> entry = iterator.next();
            CraftoEntity crafto = entry.getValue();
            
            if (!crafto.isAlive() || crafto.isRemoved()) {
                iterator.remove();
                craftosByUUID.remove(crafto.getUUID());
                CraftoMod.LOGGER.info("Cleaned up Steve: {}", entry.getKey());
            }
        }
    }
}

