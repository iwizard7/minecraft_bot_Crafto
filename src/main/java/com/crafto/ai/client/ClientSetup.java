package com.crafto.ai.client;

import com.crafto.ai.CraftoMod;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.crafto.ai.entity.CraftoEntity;

/**
 * Client-side setup for entity renderers and other client-only initialization
 */
@Mod.EventBusSubscriber(modid = CraftoMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    private static final ResourceLocation STEVE_TEXTURE = new ResourceLocation("minecraft", "textures/entity/player/wide/crafto.png");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {        event.enqueueWork(() -> {        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {        event.registerEntityRenderer(CraftoMod.CRAFTO_ENTITY.get(), context -> 
            new HumanoidMobRenderer<CraftoEntity, PlayerModel<CraftoEntity>>(
                context,
                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false),
                0.5F
            ) {
                @Override
                public ResourceLocation getTextureLocation(CraftoEntity entity) {
                    return STEVE_TEXTURE;
                }
            }
        );    }
}

