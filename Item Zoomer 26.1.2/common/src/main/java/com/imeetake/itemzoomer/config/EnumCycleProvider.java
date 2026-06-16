package com.imeetake.itemzoomer.config;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SelectorBuilder;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class EnumCycleProvider implements GuiProvider {

    @Override
    public List<AbstractConfigListEntry> get(String i13n, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        ConfigEntryBuilder builder = ConfigEntryBuilder.create();

        try {
            Enum<?> currentValue = (Enum<?>) field.get(config);
            Enum<?> defaultValue = (Enum<?>) field.get(defaults);
            Enum<?>[] values = (Enum<?>[]) field.getType().getEnumConstants();

            SelectorBuilder<Enum<?>> selectorBuilder = builder.startSelector(
                    Component.translatable(i13n),
                    values,
                    currentValue
            );

            selectorBuilder.setDefaultValue(defaultValue);
            selectorBuilder.setSaveConsumer(newValue -> {
                try {
                    field.set(config, newValue);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            });

            selectorBuilder.setNameProvider(value -> {
                String enumName = ((Enum<?>) value).name().toLowerCase();
                String className = field.getType().getSimpleName().toLowerCase();
                return Component.translatable("text.autoconfig.itemzoomer.enum." + className + "." + enumName);
            });

            return Collections.singletonList(selectorBuilder.build());

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}