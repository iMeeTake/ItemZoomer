package com.imeetake.itemzoomer.mixin;

import com.imeetake.itemzoomer.compat.TooltipStackCapture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public class ItemTooltipCaptureMixin {

    @Inject(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
    private void itemzoomer$captureTooltipStack(Font font, ItemStack stack, int x, int y, CallbackInfo ci) {
        TooltipStackCapture.record(stack);
    }
}
