package com.imeetake.itemzoomer.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

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

    boolean isAvailable();

    String getModName();
}
