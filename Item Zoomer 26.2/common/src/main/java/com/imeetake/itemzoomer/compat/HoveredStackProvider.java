package com.imeetake.itemzoomer.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface HoveredStackProvider {

    @Nullable
    ItemStack getHoveredStack(double mouseX, double mouseY);

    @Nullable
    default AbstractContainerScreen<?> getBackingScreen() {
        return null;
    }

    @Nullable
    default Rect2i getWindowBounds() {
        return null;
    }

    default List<Rect2i> getExclusionBounds() {
        return List.of();
    }

    default boolean shouldDeferAbove() {
        return false;
    }

    boolean isAvailable();

    String getModName();
}
