package com.imeetake.itemzoomer.mixin;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PictureInPictureRenderer.class)
public interface PIPRendererAccessor {

    @Accessor("textureView")
    GpuTextureView itemzoomer$getTextureView();
}