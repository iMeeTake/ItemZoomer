package com.imeetake.itemzoomer.compat;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class TooltipStackCapture {

    private static ItemStack currentFrameStack;
    private static ItemStack previousFrameStack;

    private TooltipStackCapture() {
    }

    public static void record(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            currentFrameStack = stack;
        }
    }

    public static void beginFrame() {
        previousFrameStack = currentFrameStack;
        currentFrameStack = null;
    }

    @Nullable
    public static ItemStack previousFrameStack() {
        return previousFrameStack;
    }

    public static void clear() {
        currentFrameStack = null;
        previousFrameStack = null;
    }
}
