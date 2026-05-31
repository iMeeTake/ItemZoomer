package com.imeetake.itemzoomer.forge;

import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.imeetake.itemzoomer.config.EnumCycleProvider;
import dev.architectury.platform.forge.EventBuses;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.imeetake.itemzoomer.ItemZoomer;

@Mod(ItemZoomer.MOD_ID)
public final class ItemZoomerForge {
    public ItemZoomerForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(ItemZoomer.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInit.init();
        }
    }

    private static final class ClientInit {
        private static void init() {
            ItemZoomer.init();
            registerConfigGui();

            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (client, parent) -> AutoConfig.getConfigScreen(ItemZoomerConfig.class, parent).get()
                    )
            );
        }

        private static void registerConfigGui() {
            GuiRegistry registry = AutoConfig.getGuiRegistry(ItemZoomerConfig.class);
            registry.registerPredicateProvider(new EnumCycleProvider(), field -> field.getType().isEnum());
        }
    }
}
