package com.imeetake.itemzoomer;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "itemzoomer")
public class ItemZoomerConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;
}
