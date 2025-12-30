package com.imeetake.itemzoomer.render;

import net.minecraft.world.item.ItemStack;

public class AnimationState {

    private ItemStack currentStack = ItemStack.EMPTY;
    private float animationStartTime = 0;
    private boolean isActive = false;
    private float currentTime = 0;
    private long lastNanoTime = 0;
    private int stableFrames = 0;

    private static final float APPEAR_DURATION = 0.3f;
    private static final int MIN_STABLE_FRAMES = 2;

    public void beginFrame() {
        long now = System.nanoTime();
        if (lastNanoTime > 0) {
            float deltaSec = (now - lastNanoTime) / 1_000_000_000f;
            currentTime += deltaSec;
        }
        lastNanoTime = now;
    }

    public void update(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            reset();
            return;
        }

        if (!ItemStack.matches(currentStack, stack)) {
            currentStack = stack.copy();
            animationStartTime = currentTime;
            isActive = true;
            stableFrames = 0;
        } else if (isActive) {
            stableFrames++;
        }
    }

    public void reset() {
        currentStack = ItemStack.EMPTY;
        animationStartTime = 0;
        isActive = false;
        stableFrames = 0;
    }

    public boolean isStable() {
        return stableFrames >= MIN_STABLE_FRAMES;
    }

    public float getAppearProgress(boolean hasAnimation, int delayMs) {
        if (!isActive || !isStable()) return 0.0f;

        float elapsed = currentTime - animationStartTime;
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
        if (!isActive || !isStable()) return 0.0f;
        return currentTime - animationStartTime;
    }

    public float getIdleAnimationTime(boolean hasAppearAnimation, int delayMs) {
        if (!isActive || !isStable()) return 0.0f;
        float elapsed = currentTime - animationStartTime;
        float delaySec = delayMs / 1000.0f;
        if (hasAppearAnimation && delaySec > 0) {
            float delayedElapsed = elapsed - delaySec;
            if (delayedElapsed <= 0) return 0.0f;
            return delayedElapsed;
        }
        return elapsed;
    }

    public boolean isActive() {
        return isActive && isStable();
    }

    public ItemStack getCurrentStack() {
        return currentStack;
    }

    public boolean shouldShowInfo(int delaySeconds) {
        return isStable() && getHoverDuration() >= delaySeconds;
    }

    public float getTextAppearProgress(int delaySeconds) {
        if (!isStable()) return 0.0f;
        float hoverDuration = getHoverDuration();
        if (hoverDuration < delaySeconds) return 0.0f;

        float textAnimDuration = 0.3f;
        float timeSinceDelay = hoverDuration - delaySeconds;
        return Math.min(1.0f, timeSinceDelay / textAnimDuration);
    }
}