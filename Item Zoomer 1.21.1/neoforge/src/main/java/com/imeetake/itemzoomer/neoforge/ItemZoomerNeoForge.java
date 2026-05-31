package com.imeetake.itemzoomer.neoforge;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.config.EnumCycleProvider;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(ItemZoomer.MOD_ID)
public final class ItemZoomerNeoForge {
    public ItemZoomerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInit.init(modContainer);
        }
    }

    private static final class ClientInit {
        private static void init(ModContainer modContainer) {
            ItemZoomer.init();
            registerConfigGui();

            modContainer.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (container, parent) -> AutoConfig.getConfigScreen(ItemZoomerConfig.class, parent).get()
            );
        }

        private static void registerConfigGui() {
            GuiRegistry registry = AutoConfig.getGuiRegistry(ItemZoomerConfig.class);
            registry.registerPredicateProvider(new EnumCycleProvider(), field -> field.getType().isEnum());
        }
    }
}
