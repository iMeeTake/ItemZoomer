package com.imeetake.itemzoomer.render;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.world.item.ItemStack;

public class AnimationState {

    private ItemStack currentStack = ItemStack.EMPTY;
    private double animationStartTime = 0;
    private boolean isActive = false;

    private static final float APPEAR_DURATION = 0.3f;

    public void beginFrame() {
    }

    public void update(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            reset();
            return;
        }

        if (!ItemStack.matches(currentStack, stack)) {
            currentStack = stack.copy();
            animationStartTime = Blaze3D.getTime();
            isActive = true;
        }
    }

    public void reset() {
        currentStack = ItemStack.EMPTY;
        animationStartTime = 0;
        isActive = false;
    }

    private float getElapsed() {
        return (float) (Blaze3D.getTime() - animationStartTime);
    }

    public float getAppearProgress(boolean hasAnimation, int delayMs) {
        if (!isActive) return 0.0f;

        float elapsed = getElapsed();
        float delaySec = delayMs / 1000.0f;

        if (hasAnimation && delaySec > 0) {
            float delayedElapsed = elapsed - delaySec;
            if (delayedElapsed <= 0) return 0.0f;
            return Math.min(1.0f, delayedElapsed / APPEAR_DURATION);
        } else {
            return Math.min(1.0f, elapsed / APPEAR_DURATION);
        }
    }

    public float getHoverDuration() {
        if (!isActive) return 0.0f;
        return getElapsed();
    }

    public float getIdleAnimationTime(boolean hasAppearAnimation, int delayMs) {
        if (!isActive) return 0.0f;
        float elapsed = getElapsed();
        float delaySec = delayMs / 1000.0f;
        if (hasAppearAnimation && delaySec > 0) {
            float delayedElapsed = elapsed - delaySec;
            if (delayedElapsed <= 0) return 0.0f;
            return delayedElapsed;
        }
        return elapsed;
    }

    public boolean isActive() {
        return isActive;
    }

    public ItemStack getCurrentStack() {
        return currentStack;
    }

    public boolean shouldShowInfo(int delaySeconds) {
        return isActive && getHoverDuration() >= delaySeconds;
    }

    public float getTextAppearProgress(int delaySeconds) {
        if (!isActive) return 0.0f;
        float hoverDuration = getHoverDuration();
        if (hoverDuration < delaySeconds) return 0.0f;

        float textAnimDuration = 0.3f;
        float timeSinceDelay = hoverDuration - delaySeconds;
        return Math.min(1.0f, timeSinceDelay / textAnimDuration);
    }
}