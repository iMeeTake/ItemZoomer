package com.imeetake.itemzoomer.forge;

import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import dev.architectury.platform.forge.EventBuses;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.imeetake.itemzoomer.ItemZoomer;

@Mod(ItemZoomer.MOD_ID)
public final class ItemZoomerForge {
    public ItemZoomerForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(ItemZoomer.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        ItemZoomer.init();

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> AutoConfig.getConfigScreen(ItemZoomerConfig.class, parent).get()
                )
        );
    }
}
