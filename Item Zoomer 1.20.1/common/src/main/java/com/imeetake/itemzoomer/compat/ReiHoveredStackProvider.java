package com.imeetake.itemzoomer.compat;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ReiHoveredStackProvider implements HoveredStackProvider {

    @Override
    @Nullable
    public ItemStack getHoveredStack(double mouseX, double mouseY) {
        try {
            return ReiHoveredStackHelper.getStack(mouseX, mouseY);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    @Nullable
    public Rect2i getWindowBounds() {
        try {
            return ReiHoveredStackHelper.getWindowBounds();
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String getModName() {
        return "REI";
    }
}