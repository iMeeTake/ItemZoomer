package com.imeetake.itemzoomer.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EmiHoveredStackProvider implements HoveredStackProvider {

    @Override
    @Nullable
    public ItemStack getHoveredStack(double mouseX, double mouseY) {
        try {
            return EmiHoveredStackHelper.getStack(mouseX, mouseY);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("dev.emi.emi.api.EmiApi", false, EmiHoveredStackProvider.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public String getModName() {
        return "EMI";
    }

    @Override
    @Nullable
    public AbstractContainerScreen<?> getBackingScreen() {
        try {
            return EmiHoveredStackHelper.getBackingScreen();
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public List<Rect2i> getExclusionBounds() {
        try {
            return EmiHoveredStackHelper.getExclusionBounds();
        } catch (Throwable e) {
            return List.of();
        }
    }
}
