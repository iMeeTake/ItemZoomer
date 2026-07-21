package com.imeetake.itemzoomer.mixin;

import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import net.minecraft.client.Minecraft;
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
    private void itemzoomer$renderZoomBelowTooltip(GuiGraphics guiGraphics, CallbackInfo ci) {
        if ((Object) this instanceof AbstractContainerScreen) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != (Object) this) {
            return;
        }

        int screenWidth = mc.getWindow().getScreenWidth();
        int screenHeight = mc.getWindow().getScreenHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        int mouseX = (int) (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / screenWidth);
        int mouseY = (int) (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / screenHeight);

        ZoomedItemRenderer.render(guiGraphics, (Screen) (Object) this, mouseX, mouseY);
    }
}
