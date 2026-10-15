package com.imeetake.itemzoomer.mixin;

import com.imeetake.itemzoomer.accessor.ContainerScreenAccessor;
import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements ContainerScreenAccessor {

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    protected int imageWidth;

    @Shadow
    protected int imageHeight;

    @Override
    public @Nullable Slot itemzoomer$getHoveredSlot() {
        return this.hoveredSlot;
    }

    @Override
    public int itemzoomer$getLeftPos() {
        return this.leftPos;
    }

    @Override
    public int itemzoomer$getTopPos() {
        return this.topPos;
    }

    @Override
    public int itemzoomer$getImageWidth() {
        return this.imageWidth;
    }

    @Override
    public int itemzoomer$getImageHeight() {
        return this.imageHeight;
    }

    @Inject(method = "renderContents", at = @At("TAIL"))
    private void itemzoomer$renderZoomBelowTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ZoomedItemRenderer.render(guiGraphics, (Screen) (Object) this, mouseX, mouseY);
    }
}