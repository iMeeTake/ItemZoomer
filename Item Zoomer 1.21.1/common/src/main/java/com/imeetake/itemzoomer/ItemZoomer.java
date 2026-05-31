package com.imeetake.itemzoomer;

import com.imeetake.itemzoomer.compat.HoveredStackProviderRegistry;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;

public class ItemZoomer {
    public static final String MOD_ID = "itemzoomer";

    public static boolean isEnabled() {
        return ItemZoomerConfig.get().enabled;
    }

    public static void toggle() {
        ItemZoomerConfig.get().toggleEnabled();
    }

    public static void init() {
        ItemZoomerConfig.init();
        HoveredStackProviderRegistry.init();
    }
}