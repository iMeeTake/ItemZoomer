package com.imeetake.itemzoomer.mixin.client;

import com.imeetake.itemzoomer.ItemZoomerClient;
import com.imeetake.itemzoomer.render.ZoomRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.imeetake.itemzoomer.ItemZoomerClient.CONFIG;


@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderZoomOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (CONFIG.enabled()) {
            ZoomRenderer.render(context);
        }
    }
}