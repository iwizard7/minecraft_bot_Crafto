package com.crafto.ai;

import com.mojang.logging.LogUtils;
import com.crafto.ai.command.CraftoCommands;
import com.crafto.ai.config.CraftoConfig;
import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.entity.CraftoManager;
import com.crafto.ai.optimization.PerformanceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(CraftoMod.MODID)
public class CraftoMod {
    public static final String MODID = "steve";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<CraftoEntity>> CRAFTO_ENTITY = ENTITIES.register("steve",
        () -> EntityType.Builder.of(CraftoEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.8F)
            .clientTrackingRange(10)
            .build("steve"));

    private static CraftoManager steveManager;
    private static PerformanceManager performanceManager;

    public CraftoMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ENTITIES.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CraftoConfig.SPEC);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::entityAttributes);

        MinecraftForge.EVENT_BUS.register(this);
        
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(com.crafto.ai.client.CraftoGUI.class);        }
        
        steveManager = new CraftoManager();
        performanceManager = PerformanceManager.getInstance();
        
        LOGGER.info("Crafto AI Mod initialized with performance optimizations");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Инициализация системы оптимизации
        performanceManager.setAdaptiveOptimization(true);
        performanceManager.setMaxConcurrentRequests(3);
        performanceManager.setCacheExpirationTime(30 * 60 * 1000L); // 30 минут
        
        LOGGER.info("Performance optimization system initialized");
    }

    private void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(CRAFTO_ENTITY.get(), CraftoEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {        CraftoCommands.register(event.getDispatcher());    }

    public static CraftoManager getCraftoManager() {
        return steveManager;
    }
    
    public static PerformanceManager getPerformanceManager() {
        return performanceManager;
    }
}

