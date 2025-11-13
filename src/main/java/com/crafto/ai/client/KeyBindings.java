package com.crafto.ai.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.crafto.ai.CraftoMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = CraftoMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {
    
    public static final String KEY_CATEGORY = "key.categories.crafto";
    
    public static KeyMapping TOGGLE_GUI;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        TOGGLE_GUI = new KeyMapping(
            "key.crafto.toggle_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, // K key
            KEY_CATEGORY
        );
        
        event.register(TOGGLE_GUI);    }
}

