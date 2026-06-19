package com.imeetake.itemzoomer.fabric.mixin;

import com.imeetake.itemzoomer.render.ZoomedItemPIPRenderer;
import com.imeetake.itemzoomer.render.ZoomedItemRenderState;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @Shadow
    @Final
    @Mutable
    private Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> pictureInPictureRenderers;

    @Unique
    private ZoomedItemPIPRenderer itemzoomer$zoomedItemRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void itemzoomer$onInit(GuiRenderState guiRenderState, FeatureRenderDispatcher featureRenderDispatcher, List<PictureInPictureRenderer<?>> list, CallbackInfo ci) {
        itemzoomer$zoomedItemRenderer = new ZoomedItemPIPRenderer();
        Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> renderers = new HashMap<>(this.pictureInPictureRenderers);
        renderers.put(ZoomedItemRenderState.class, itemzoomer$zoomedItemRenderer);
        this.pictureInPictureRenderers = renderers;
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void itemzoomer$onClose(CallbackInfo ci) {
        if (itemzoomer$zoomedItemRenderer != null) {
            itemzoomer$zoomedItemRenderer.close();
        }
    }
}
