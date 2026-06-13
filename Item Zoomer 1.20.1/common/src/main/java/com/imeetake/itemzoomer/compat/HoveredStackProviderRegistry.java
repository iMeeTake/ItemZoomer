package com.imeetake.itemzoomer.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HoveredStackProviderRegistry {

    private static final List<HoveredStackProvider> providers = new ArrayList<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        tryRegister("com.imeetake.itemzoomer.compat.JeiHoveredStackProvider");
        tryRegister("com.imeetake.itemzoomer.compat.ReiHoveredStackProvider");
        tryRegister("com.imeetake.itemzoomer.compat.EmiHoveredStackProvider");
    }

    private static void tryRegister(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            HoveredStackProvider provider = (HoveredStackProvider) clazz.getDeclaredConstructor().newInstance();
            if (provider.isAvailable()) {
                providers.add(provider);
            }
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    public static ItemStack getHoveredStack(double mouseX, double mouseY) {
        if (!initialized) {
            init();
        }
        for (HoveredStackProvider provider : providers) {
            try {
                ItemStack stack = provider.getHoveredStack(mouseX, mouseY);
                if (stack != null && !stack.isEmpty()) {
                    return stack;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Nullable
    public static AbstractContainerScreen<?> getBackingScreen() {
        if (!initialized) {
            init();
        }
        for (HoveredStackProvider provider : providers) {
            try {
                AbstractContainerScreen<?> screen = provider.getBackingScreen();
                if (screen != null) {
                    return screen;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Nullable
    public static Rect2i getWindowBounds() {
        if (!initialized) {
            init();
        }
        for (HoveredStackProvider provider : providers) {
            try {
                Rect2i bounds = provider.getWindowBounds();
                if (bounds != null) {
                    return bounds;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static List<Rect2i> getExclusionBounds() {
        if (!initialized) {
            init();
        }
        List<Rect2i> bounds = new ArrayList<>();
        for (HoveredStackProvider provider : providers) {
            try {
                List<Rect2i> providerBounds = provider.getExclusionBounds();
                if (providerBounds != null) {
                    bounds.addAll(providerBounds);
                }
            } catch (Throwable ignored) {
            }
        }
        return bounds;
    }

    public static boolean shouldDeferAbove() {
        if (!initialized) {
            init();
        }
        for (HoveredStackProvider provider : providers) {
            try {
                if (provider.shouldDeferAbove()) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static boolean hasProviders() {
        if (!initialized) {
            init();
        }
        return !providers.isEmpty();
    }
}
