package com.imeetake.itemzoomer.mixin;

import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.BookmarkOverlay", remap = false)
public class JeiBookmarkOverlayMixin {

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void itemzoomer$renderZoomAbove(Minecraft minecraft, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ItemZoomerConfig.get().favoritesOverlap != ItemZoomerConfig.FavoritesOverlap.ABOVE) {
            return;
        }
        Screen screen = minecraft.screen;
        if (screen != null) {
            ZoomedItemRenderer.renderFromViewer(guiGraphics, screen, mouseX, mouseY);
        }
    }
}
