package com.imeetake.itemzoomer.fabric;

import com.imeetake.itemzoomer.ItemZoomer;
import net.fabricmc.api.ModInitializer;

public final class ItemZoomerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ItemZoomer.init();
    }
}
