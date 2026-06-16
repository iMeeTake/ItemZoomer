package com.imeetake.itemzoomer.mixin;

import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl", remap = false)
public class ReiScreenOverlayMixin {

    @Inject(method = "renderWidgets", at = @At("HEAD"), require = 0)
    private void itemzoomer$renderZoomBelow(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ItemZoomerConfig.get().favoritesOverlap == ItemZoomerConfig.FavoritesOverlap.BELOW) {
            itemzoomer$renderZoom(graphics, mouseX, mouseY);
        }
    }

    @Inject(method = "renderWidgets", at = @At("TAIL"), require = 0)
    private void itemzoomer$renderZoomAbove(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ItemZoomerConfig.FavoritesOverlap mode = ItemZoomerConfig.get().favoritesOverlap;
        if (mode == ItemZoomerConfig.FavoritesOverlap.ABOVE || mode == ItemZoomerConfig.FavoritesOverlap.HIDE) {
            itemzoomer$renderZoom(graphics, mouseX, mouseY);
        }
    }

    private static void itemzoomer$renderZoom(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            ZoomedItemRenderer.renderFromViewer(graphics, screen, mouseX, mouseY);
        }
    }
}
