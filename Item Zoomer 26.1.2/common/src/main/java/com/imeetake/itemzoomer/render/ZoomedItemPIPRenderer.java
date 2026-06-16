package com.imeetake.itemzoomer.render;

import com.imeetake.itemzoomer.mixin.PictureInPictureRendererAccessor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;

public class ZoomedItemPIPRenderer extends PictureInPictureRenderer<ZoomedItemRenderState> {

    public ZoomedItemPIPRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<ZoomedItemRenderState> getRenderStateClass() {
        return ZoomedItemRenderState.class;
    }

    @Override
    protected void renderToTexture(ZoomedItemRenderState state, PoseStack poseStack) {
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(state.modelOffsetX(), state.modelOffsetY(), 0.0F);
        poseStack.scale(state.contentScale(), state.contentScale(), state.contentScale());

        if (state.rotation() != 0) {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(state.rotation()));
        }

        ItemStackRenderState itemState = state.itemStackRenderState();

        boolean usesBlockLight = itemState.usesBlockLight();
        if (usesBlockLight) {
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        } else {
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        }

        SubmitNodeCollector submitNodeCollector = Minecraft.getInstance().gameRenderer.getSubmitNodeStorage();
        FeatureRenderDispatcher featureRenderDispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();

        try {
            itemState.submit(poseStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
            featureRenderDispatcher.renderAllFeatures();
        } catch (Throwable t) {
            ZoomedItemRenderer.logCurrentRenderFailure(t);
        }
    }

    @Override
    protected void blitTexture(ZoomedItemRenderState state, GuiRenderState guiRenderState) {
        GpuTextureView textureView = ((PictureInPictureRendererAccessor) this).itemzoomer$getTextureView();
        if (textureView == null) {
            return;
        }

        float alpha = state.alpha();
        int intAlpha = Math.min(255, Math.max(0, (int) (alpha * 255)));

        int r = (int) (255 * alpha);
        int g = (int) (255 * alpha);
        int b = (int) (255 * alpha);
        int color = ARGB.color(intAlpha, r, g, b);

        guiRenderState.addBlitToCurrentLayer(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
                state.pose(),
                (int) state.renderX0(),
                (int) state.renderY0(),
                (int) Math.ceil(state.renderX1()),
                (int) Math.ceil(state.renderY1()),
                0.0F,
                1.0F,
                1.0F,
                0.0F,
                color,
                state.scissorArea(),
                (ScreenRectangle) null
        ));
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return (float) height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "itemzoomer_zoomed";
    }

    @Override
    protected boolean textureIsReadyToBlit(ZoomedItemRenderState state) {
        return false;
    }
}
