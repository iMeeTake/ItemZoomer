package com.imeetake.itemzoomer.config;

import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.gui.registry.api.GuiTransformer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.List;

public class SliderValueTextTransformer implements GuiTransformer {

    @Override
    public List<AbstractConfigListEntry> transform(List<AbstractConfigListEntry> guis, String i13n, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        String valueKey = i13n + ".value";
        for (AbstractConfigListEntry entry : guis) {
            if (entry instanceof IntegerSliderEntry slider) {
                slider.setTextGetter(value -> Component.translatable(valueKey, value));
            }
        }
        return guis;
    }
}
