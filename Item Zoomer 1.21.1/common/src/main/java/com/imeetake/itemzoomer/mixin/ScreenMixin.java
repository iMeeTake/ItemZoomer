package com.imeetake.itemzoomer.mixin;

import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void itemzoomer$renderZoomBelowTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if ((Object) this instanceof AbstractContainerScreen) {
            return;
        }

        ZoomedItemRenderer.render(guiGraphics, (Screen) (Object) this, mouseX, mouseY);
    }
}
