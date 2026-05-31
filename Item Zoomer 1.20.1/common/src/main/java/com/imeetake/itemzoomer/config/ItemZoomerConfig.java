package com.imeetake.itemzoomer.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Config(name = "itemzoomer")
public class ItemZoomerConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    private static ItemZoomerConfig INSTANCE;

    public static void init() {
        AutoConfig.register(ItemZoomerConfig.class, GsonConfigSerializer::new);
        INSTANCE = AutoConfig.getConfigHolder(ItemZoomerConfig.class).getConfig();
    }

    public static ItemZoomerConfig get() {
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

    @Override
    public void validatePostLoad() throws ConfigData.ValidationException {
        appearDelayMs = clamp(appearDelayMs, 0, 1000);
        itemSizePercent = clamp(itemSizePercent, 50, 150);
        infoDelaySeconds = clamp(infoDelaySeconds, 0, 10);

        if (appearAnimation == null) {
            appearAnimation = AppearAnimation.FADE;
        }

        if (idleAnimation == null) {
            idleAnimation = IdleAnimation.NONE;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
