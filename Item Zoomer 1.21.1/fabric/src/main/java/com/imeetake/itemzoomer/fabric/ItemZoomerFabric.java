package com.imeetake.itemzoomer.fabric;

import net.fabricmc.api.ModInitializer;

import com.imeetake.itemzoomer.ItemZoomer;

public final class ItemZoomerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ItemZoomer.init();
    }
}
