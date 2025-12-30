package com.imeetake.itemzoomer.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
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

        itemState.render(poseStack, this.bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
    }

    @Override
    public void blitTexture(ZoomedItemRenderState state, GuiRenderState guiRenderState) {
        float alpha = state.alpha();
        int intAlpha = (int) (alpha * 255);
        int color = (intAlpha << 24) | 0xFFFFFF;

        guiRenderState.submitBlitToCurrentLayer(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(getTextureView()),
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

    protected com.mojang.blaze3d.textures.GpuTextureView getTextureView() {
        try {
            java.lang.reflect.Field field = PictureInPictureRenderer.class.getDeclaredField("textureView");
            field.setAccessible(true);
            return (com.mojang.blaze3d.textures.GpuTextureView) field.get(this);
        } catch (Exception e) {
            return null;
        }
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