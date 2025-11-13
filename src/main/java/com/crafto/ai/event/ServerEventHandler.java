package com.crafto.ai.event;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.entity.CraftoManager;
import com.crafto.ai.memory.StructureRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.ServerChatEvent;

@Mod.EventBusSubscriber(modid = CraftoMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {
    private static boolean stevesSpawned = false;

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            CraftoManager manager = CraftoMod.getCraftoManager();
            if (!stevesSpawned) {                manager.clearAllCraftos();
                
                // Clear structure registry for fresh spatial awareness
                StructureRegistry.clear();
                
                // Then, remove ALL CraftoEntity instances from the world (including ones loaded from NBT)
                int removedCount = 0;
                for (var entity : level.getAllEntities()) {
                    if (entity instanceof CraftoEntity) {
                        entity.discard();
                        removedCount++;
                    }
                }                Vec3 playerPos = player.position();
                Vec3 lookVec = player.getLookAngle();
                
                String[] names = {"Alex"};

                for (int i = 0; i < 1; i++) {
                    double offsetX = lookVec.x * 5;
                    double offsetZ = lookVec.z * 5;

                    Vec3 spawnPos = new Vec3(
                        playerPos.x + offsetX,
                        playerPos.y,
                        playerPos.z + offsetZ
                    );

                    CraftoEntity crafto = manager.spawnCrafto(level, spawnPos, names[i]);
                    if (crafto != null) {                    }
                }
                
                stevesSpawned = true;            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        stevesSpawned = false;
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        String message = event.getMessage().getString();

        // Check if message starts with @ followed by a Steve name
        if (message.startsWith("@")) {
            String[] parts = message.split(" ", 2);
            if (parts.length >= 2) {
                String targetName = parts[0].substring(1); // Remove @
                String command = parts[1];

                CraftoManager manager = CraftoMod.getCraftoManager();
                CraftoEntity crafto = manager.getCrafto(targetName);

                if (crafto != null) {
                    // Process the command for the targeted Steve
                    new Thread(() -> {
                        crafto.getActionExecutor().processNaturalLanguageCommand(command);
                    }).start();

                    // Optionally cancel the chat message so it doesn't appear in chat
                    // event.setCanceled(true);
                }
            }
        }
    }
}
