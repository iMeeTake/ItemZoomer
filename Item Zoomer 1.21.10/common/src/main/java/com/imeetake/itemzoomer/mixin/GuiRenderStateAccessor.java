package com.imeetake.itemzoomer.mixin;

import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiRenderState.class)
public interface GuiRenderStateAccessor {
    @Invoker("submitPicturesInPictureState")
    void itemzoomer$submitPictureInPicture(PictureInPictureRenderState state);
}