package com.imeetake.itemzoomer.mixin;

import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiScreenManager.class, remap = false)
public class EmiScreenManagerMixin {

    @Inject(method = "render(Ldev/emi/emi/runtime/EmiDrawContext;IIF)V", at = @At("HEAD"), remap = false)
    private static void itemzoomer$renderZoomBelow(EmiDrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ItemZoomerConfig.get().favoritesOverlap != ItemZoomerConfig.FavoritesOverlap.BELOW) {
            return;
        }
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            ZoomedItemRenderer.renderFromViewer(context.raw(), screen, mouseX, mouseY);
        }
    }
}
