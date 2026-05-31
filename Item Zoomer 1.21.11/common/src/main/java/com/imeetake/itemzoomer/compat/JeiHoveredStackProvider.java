package com.imeetake.itemzoomer.compat;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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
    public AbstractContainerScreen<?> getBackingScreen() {
        try {
            return JeiHoveredStackHelper.getBackingScreen();
        } catch (Throwable e) {
            return null;
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
