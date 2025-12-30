package com.imeetake.itemzoomer.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;

@Config(name = "itemzoomer")
public class ItemZoomerConfig implements ConfigData {

    private static ItemZoomerConfig INSTANCE;

    public static void init() {
        AutoConfig.register(ItemZoomerConfig.class, GsonConfigSerializer::new);
        INSTANCE = AutoConfig.getConfigHolder(ItemZoomerConfig.class).getConfig();

        GuiRegistry registry = AutoConfig.getGuiRegistry(ItemZoomerConfig.class);
        registry.registerPredicateProvider(new EnumCycleProvider(), field -> field.getType().isEnum());
    }

    public static ItemZoomerConfig get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("You forgot to initialize the config again, iMeeTake! Hey, if you're reading this :D");
        }
        return INSTANCE;
    }

    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Gui.Tooltip
    public AppearAnimation appearAnimation = AppearAnimation.FADE;

    @ConfigEntry.Gui.Tooltip
    public IdleAnimation idleAnimation = IdleAnimation.NONE;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
    public int appearDelayMs = 250;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 50, max = 150)
    public int itemSizePercent = 100;

    @ConfigEntry.Gui.Tooltip
    public boolean showItemInfo = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 10)
    public int infoDelaySeconds = 2;

    public void toggleEnabled() {
        this.enabled = !this.enabled;
        AutoConfig.getConfigHolder(ItemZoomerConfig.class).save();
    }

    public enum AppearAnimation {
        NONE,
        FADE,
        SCALE,
        SLIDE_LEFT,
        SLIDE_RIGHT,
        SLIDE_TOP,
        SLIDE_BOTTOM
    }

    public enum IdleAnimation {
        NONE,
        SWING,
        PULSE
    }
}