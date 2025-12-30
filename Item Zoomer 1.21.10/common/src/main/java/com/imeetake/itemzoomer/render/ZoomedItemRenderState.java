package com.imeetake.itemzoomer.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record ZoomedItemRenderState(
        ItemStackRenderState itemStackRenderState,
        float renderX0,
        float renderY0,
        float renderX1,
        float renderY1,
        float rotation,
        float alpha,
        @Nullable ScreenRectangle scissor
) implements PictureInPictureRenderState {

    @Override
    public int x0() {
        return (int) renderX0;
    }

    @Override
    public int y0() {
        return (int) renderY0;
    }

    @Override
    public int x1() {
        return (int) Math.ceil(renderX1);
    }

    @Override
    public int y1() {
        return (int) Math.ceil(renderY1);
    }

    @Override
    public float scale() {
        return renderX1 - renderX0;
    }

    @Override
    public Matrix3x2f pose() {
        return IDENTITY_POSE;
    }

    @Nullable
    @Override
    public ScreenRectangle scissorArea() {
        return scissor;
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        return PictureInPictureRenderState.getBounds(x0(), y0(), x1(), y1(), scissor);
    }
}