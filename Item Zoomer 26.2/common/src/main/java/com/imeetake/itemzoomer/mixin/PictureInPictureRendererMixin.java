package com.imeetake.itemzoomer.mixin;

import com.imeetake.itemzoomer.render.ZoomedItemPIPRenderer;
import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PictureInPictureRenderer.class)
public class PictureInPictureRendererMixin {

    @Redirect(
            method = "prepare",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V"
            )
    )
    private void itemzoomer$guardRenderAllFeatures(FeatureRenderDispatcher dispatcher, SubmitNodeStorage storage) {
        if ((Object) this instanceof ZoomedItemPIPRenderer) {
            try {
                dispatcher.renderAllFeatures(storage);
            } catch (Throwable t) {
                ZoomedItemRenderer.logCurrentRenderFailure(t);
            }
        } else {
            dispatcher.renderAllFeatures(storage);
        }
    }
}
