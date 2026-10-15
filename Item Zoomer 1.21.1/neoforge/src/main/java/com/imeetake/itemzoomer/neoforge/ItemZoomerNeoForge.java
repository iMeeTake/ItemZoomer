package com.imeetake.itemzoomer.neoforge;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.config.EnumCycleProvider;
import com.imeetake.itemzoomer.config.SliderValueTextTransformer;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(ItemZoomer.MOD_ID)
public final class ItemZoomerNeoForge {
    public ItemZoomerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInit.init(modEventBus, modContainer);
        }
    }

    private static final class ClientInit {
        private static void init(IEventBus modEventBus, ModContainer modContainer) {
            modEventBus.addListener(ClientInit::onConstructMod);

            modContainer.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (container, parent) -> AutoConfig.getConfigScreen(ItemZoomerConfig.class, parent).get()
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
