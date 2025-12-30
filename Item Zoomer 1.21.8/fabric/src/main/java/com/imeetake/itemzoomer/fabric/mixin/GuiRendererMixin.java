package com.imeetake.itemzoomer.fabric.mixin;

import com.imeetake.itemzoomer.render.ZoomedItemPIPRenderer;
import com.imeetake.itemzoomer.render.ZoomedItemRenderState;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @Shadow
    @Final
    private Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> pictureInPictureRenderers;

    @Shadow
    @Final
    private MultiBufferSource.BufferSource bufferSource;

    @Unique
    private ZoomedItemPIPRenderer itemzoomer$zoomedItemRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void itemzoomer$onInit(GuiRenderState guiRenderState, MultiBufferSource.BufferSource bufferSource, List<PictureInPictureRenderer<?>> list, CallbackInfo ci) {
        itemzoomer$zoomedItemRenderer = new ZoomedItemPIPRenderer(this.bufferSource);
        ((Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>>) pictureInPictureRenderers)
                .put(ZoomedItemRenderState.class, itemzoomer$zoomedItemRenderer);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void itemzoomer$onClose(CallbackInfo ci) {
        if (itemzoomer$zoomedItemRenderer != null) {
            itemzoomer$zoomedItemRenderer.close();
        }
    }
}