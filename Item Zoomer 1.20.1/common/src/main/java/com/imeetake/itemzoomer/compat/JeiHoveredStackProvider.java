package com.imeetake.itemzoomer.compat;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JeiHoveredStackProvider implements HoveredStackProvider {

    @Override
    @Nullable
    public ItemStack getHoveredStack(double mouseX, double mouseY) {
        try {
            return JeiHoveredStackHelper.getStack(mouseX, mouseY);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    @Nullable
    public Rect2i getWindowBounds() {
        try {
            return JeiHoveredStackHelper.getWindowBounds();
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public List<Rect2i> getExclusionBounds() {
        try {
            return JeiHoveredStackHelper.getExclusionBounds();
        } catch (Throwable e) {
            return List.of();
        }
    }

    @Override
    public boolean shouldDeferAbove() {
        try {
            return JeiHoveredStackHelper.shouldDeferAbove();
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("mezz.jei.api.runtime.IJeiRuntime");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String getModName() {
        return "JEI";
    }
}
