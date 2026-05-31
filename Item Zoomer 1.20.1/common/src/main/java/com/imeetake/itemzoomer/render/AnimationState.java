package com.imeetake.itemzoomer.render;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class AnimationState {

    private static final float APPEAR_DURATION = 0.3f;
    private static final float TEXT_FADE_DURATION = 0.3f;
    private static final float GRACE_SECONDS = 0.12f;
    private static final float MAX_DT = 0.05f;
    private static final float VISIBLE_EPSILON = 0.001f;

    private ItemStack currentStack = ItemStack.EMPTY;
    private Object currentRenderIdentity = null;
    private double animationStartTime = 0;
    private double lastPresentTime = 0;
    private double lastTickTime = 0;
    private boolean hasTimeline = false;

    private float appearProgress = 0f;
    private float textProgress = 0f;

    private boolean hasAppearAnimation = false;
    private int appearDelayMs = 0;

    public void beginFrame() {
    }

    public void update(ItemStack liveStack, Object renderIdentity,
                       boolean hasAppearAnimation, int appearDelayMs, int textDelaySeconds) {
        double now = Blaze3D.getTime();
        float dt = lastTickTime == 0 ? 0f : (float) Math.max(0.0, Math.min(MAX_DT, now - lastTickTime));
        lastTickTime = now;

        this.hasAppearAnimation = hasAppearAnimation;
        this.appearDelayMs = appearDelayMs;

        boolean present = liveStack != null && !liveStack.isEmpty();

        if (present) {
            Object nextIdentity = renderIdentity != null ? renderIdentity : liveStack.getItem();
            if (!hasTimeline || !Objects.equals(currentRenderIdentity, nextIdentity)) {
                currentRenderIdentity = nextIdentity;
                animationStartTime = now;
                appearProgress = 0f;
                textProgress = 0f;
            }
            currentStack = liveStack.copy();
            lastPresentTime = now;
            hasTimeline = true;
        } else if (hasTimeline && (now - lastPresentTime) > GRACE_SECONDS) {
            hasTimeline = false;
        }

        float appearTarget = 0f;
        float textTarget = 0f;
        if (hasTimeline) {
            double elapsed = now - animationStartTime;
            double appearDelaySec = (hasAppearAnimation && appearDelayMs > 0) ? appearDelayMs / 1000.0 : 0.0;
            appearTarget = elapsed >= appearDelaySec ? 1f : 0f;
            textTarget = elapsed >= textDelaySeconds ? 1f : 0f;
        }

        appearProgress = approach(appearProgress, appearTarget, dt / APPEAR_DURATION);
        textProgress = approach(textProgress, textTarget, dt / TEXT_FADE_DURATION);

        if (!hasTimeline && appearProgress <= 0f) {
            currentStack = ItemStack.EMPTY;
            currentRenderIdentity = null;
            animationStartTime = 0;
        }
    }

    public void reset() {
        currentStack = ItemStack.EMPTY;
        currentRenderIdentity = null;
        animationStartTime = 0;
        lastPresentTime = 0;
        lastTickTime = 0;
        hasTimeline = false;
        appearProgress = 0f;
        textProgress = 0f;
    }

    private static float approach(float current, float target, float maxStep) {
        if (maxStep <= 0f) return current;
        float diff = target - current;
        if (Math.abs(diff) <= maxStep) return target;
        return current + Math.copySign(maxStep, diff);
    }

    public ItemStack getRenderStack() {
        return currentStack;
    }

    public float getAppearProgress() {
        return appearProgress;
    }

    public float getTextProgress() {
        return textProgress;
    }

    public boolean shouldShowInfo() {
        return textProgress > VISIBLE_EPSILON;
    }

    public float getIdleAnimationTime() {
        if (!hasTimeline) return 0f;
        double elapsed = Blaze3D.getTime() - animationStartTime;
        double appearDelaySec = (hasAppearAnimation && appearDelayMs > 0) ? appearDelayMs / 1000.0 : 0.0;
        double t = elapsed - appearDelaySec;
        return t > 0 ? (float) t : 0f;
    }
}
