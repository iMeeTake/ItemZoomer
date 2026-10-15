package com.imeetake.itemzoomer.forge;

import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.imeetake.itemzoomer.config.EnumCycleProvider;
import com.imeetake.itemzoomer.config.SliderValueTextTransformer;
import dev.architectury.platform.forge.EventBuses;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.imeetake.itemzoomer.ItemZoomer;

@Mod(ItemZoomer.MOD_ID)
public final class ItemZoomerForge {
    public ItemZoomerForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(ItemZoomer.MOD_ID, modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInit.init(modEventBus);
        }
    }

    private static final class ClientInit {
        private static void init(IEventBus modEventBus) {
            modEventBus.addListener(ClientInit::onConstructMod);

            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (client, parent) -> AutoConfig.getConfigScreen(ItemZoomerConfig.class, parent).get()
                    )
            );
        }

        private static void onConstructMod(FMLConstructModEvent event) {
            event.enqueueWork(() -> {
                ItemZoomer.init();
                registerConfigGui();
            });
        }

        private static void registerConfigGui() {
            GuiRegistry registry = AutoConfig.getGuiRegistry(ItemZoomerConfig.class);
            registry.registerPredicateProvider(new EnumCycleProvider(), field -> field.getType().isEnum());
            registry.registerAnnotationTransformer(new SliderValueTextTransformer(), ConfigEntry.BoundedDiscrete.class);
        }
    }
}
