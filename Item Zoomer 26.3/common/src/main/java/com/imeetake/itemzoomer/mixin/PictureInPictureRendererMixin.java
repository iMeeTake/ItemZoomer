package com.imeetake.itemzoomer.mixin;

import com.imeetake.itemzoomer.render.ZoomedItemPIPRenderer;
import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PictureInPictureRenderer.class)
public class PictureInPictureRendererMixin {

    @WrapOperation(
            method = "prepare",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lcom/mojang/renderpearl/api/commands/RenderPass;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;)V"
            )
    )
    private void itemzoomer$guardRenderAllFeatures(RenderPass renderPass, FeatureRenderDispatcher.PreparedFrame frame, Operation<Void> original) {
        if ((Object) this instanceof ZoomedItemPIPRenderer) {
            try {
                original.call(renderPass, frame);
            } catch (Throwable t) {
                ZoomedItemRenderer.logCurrentRenderFailure(t);
            }
        } else {
            original.call(renderPass, frame);
        }
    }
}
