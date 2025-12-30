package com.imeetake.itemzoomer.render;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.accessor.ContainerScreenAccessor;
import com.imeetake.itemzoomer.accessor.RecipeBookScreenAccessor;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.joml.Matrix4f;

import java.util.List;
import java.util.OptionalInt;

public class ZoomedItemRenderer {

    private static final int PADDING = 5;
    private static final AnimationState animationState = new AnimationState();

    private static RenderTarget renderTarget = null;
    private static int cachedWidth = 0;
    private static int cachedHeight = 0;

    private static final ItemStackRenderState itemRenderState = new ItemStackRenderState();

    public static void beginFrame() {
        animationState.beginFrame();
    }

    public static void render(GuiGraphics graphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (!ItemZoomer.isEnabled()) {
            animationState.reset();
            return;
        }

        ItemZoomerConfig config = ItemZoomerConfig.get();

        if (!(screen instanceof ContainerScreenAccessor accessor)) {
            animationState.reset();
            return;
        }

        if (isLeftSideBlocked(screen, accessor)) {
            animationState.reset();
            return;
        }

        Slot hoveredSlot = accessor.itemzoomer$getHoveredSlot();

        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            animationState.reset();
            return;
        }

        ItemStack stack = hoveredSlot.getItem();
        animationState.update(stack);

        graphics.flush();

        renderZoomedItem(graphics, accessor, stack, config);
    }

    private static void renderZoomedItem(GuiGraphics graphics, ContainerScreenAccessor accessor, ItemStack stack, ItemZoomerConfig config) {
        Minecraft mc = Minecraft.getInstance();

        int guiLeft = accessor.itemzoomer$getLeftPos();
        int guiTop = accessor.itemzoomer$getTopPos();
        int guiHeight = accessor.itemzoomer$getImageHeight();

        boolean hasAppearAnimation = config.appearAnimation != ItemZoomerConfig.AppearAnimation.NONE;
        int delayMs = config.appearDelayMs;
        float progress = animationState.getAppearProgress(hasAppearAnimation, delayMs);
        if (progress <= 0) return;

        int baseSize = 115;
        int availableWidth = guiLeft - PADDING * 2;
        float sizeMultiplier = config.itemSizePercent / 100.0f;
        int itemSize = (int) (Math.min(baseSize, availableWidth) * sizeMultiplier);
        itemSize = Math.max(itemSize, 32);

        int centerX = guiLeft / 2;
        int centerY = guiTop + (guiHeight / 2);

        float itemX = centerX - (itemSize / 2.0f);
        float itemY = centerY - (itemSize / 2.0f);
        float textX = itemX;
        int textGap = 12;
        float textY = itemY + itemSize + textGap;

        float eased = easeOutCubic(progress);
        float alpha = 1.0f;
        float offsetX = 0, offsetY = 0;
        float scale = 1.0f;
        int clipWidth = 0;
        boolean useClipping = false;

        switch (config.appearAnimation) {
            case FADE -> alpha = eased;
            case SCALE -> scale = eased;
            case SLIDE_LEFT -> offsetX = (1 - eased) * -itemSize;
            case SLIDE_RIGHT -> {
                float distanceToGui = guiLeft - itemX + PADDING;
                offsetX = (1 - eased) * distanceToGui;
                useClipping = eased < 1.0f;
                clipWidth = guiLeft;
            }
            case SLIDE_TOP -> offsetY = (1 - eased) * -itemSize;
            case SLIDE_BOTTOM -> offsetY = (1 - eased) * itemSize;
            default -> {
            }
        }

        float idleTime = animationState.getIdleAnimationTime(hasAppearAnimation, delayMs);
        float idleAngle = 0;
        float idleScale = 1.0f;

        switch (config.idleAnimation) {
            case SWING -> idleAngle = (float) Math.sin(idleTime * 0.8) * 2.0f;
            case PULSE -> idleScale = 1.0f + (float) Math.sin(idleTime * 1.2) * 0.015f;
            default -> {
            }
        }

        float finalScale = scale * idleScale;
        float finalItemX = itemX + offsetX;
        float finalItemY = itemY + offsetY;
        float finalTextX = textX + offsetX;
        float finalTextY = textY + offsetY;

        boolean showInfo = config.showItemInfo && animationState.shouldShowInfo(config.infoDelaySeconds);
        float textAlpha = showInfo ? easeOutCubic(animationState.getTextAppearProgress(config.infoDelaySeconds)) : 0;

        renderWithFBO(graphics, mc, stack, finalItemX, finalItemY, itemSize, finalScale, idleAngle,
                alpha, showInfo, finalTextX, finalTextY, textAlpha, useClipping, clipWidth);
    }

    private static boolean isLeftSideBlocked(AbstractContainerScreen<?> screen, ContainerScreenAccessor accessor) {
        if (screen instanceof RecipeBookScreenAccessor recipeAccessor) {
            if (recipeAccessor.itemzoomer$isRecipeBookVisible()) {
                return true;
            }
        }

        int guiLeft = accessor.itemzoomer$getLeftPos();
        int minSpace = 50;
        return guiLeft < minSpace;
    }

    private static void renderWithFBO(GuiGraphics graphics, Minecraft mc, ItemStack stack,
                                      float itemX, float itemY, int itemSize, float scale, float rotation,
                                      float alpha, boolean showInfo, float textX, float textY, float textAlpha,
                                      boolean useClipping, int clipWidth) {

        int windowWidth = mc.getWindow().getWidth();
        int windowHeight = mc.getWindow().getHeight();
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();

        ensureRenderTarget(windowWidth, windowHeight);
        if (renderTarget == null) return;

        GpuTexture colorTexture = renderTarget.getColorTexture();
        GpuTexture depthTexture = renderTarget.getDepthTexture();
        if (colorTexture == null) return;

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(colorTexture, 0);
        if (depthTexture != null) {
            encoder.clearDepthTexture(depthTexture, 1.0);
        }

        if (useClipping) {
            double guiScale = mc.getWindow().getGuiScale();
            int scissorX = 0;
            int scissorY = 0;
            int scissorW = (int) (clipWidth * guiScale);
            int scissorH = windowHeight;
            RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        }

        ZoomedItemBufferSource.setTarget(renderTarget);

        try {
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            ZoomedItemBufferSource zoomedSource = new ZoomedItemBufferSource(bufferSource);

            renderItemToTarget(zoomedSource, mc, stack, itemX, itemY, itemSize, scale, rotation, guiWidth, guiHeight);

            zoomedSource.endBatch();

            if (showInfo && textAlpha > 0) {
                renderTextToTarget(graphics, mc, stack, textX, textY, itemSize, textAlpha);
            }

        } finally {
            ZoomedItemBufferSource.setTarget(null);
            if (useClipping) {
                RenderSystem.disableScissor();
            }
        }

        RenderTarget mainTarget = mc.getMainRenderTarget();
        blitWithAlpha(renderTarget, guiWidth, guiHeight, alpha, mainTarget);
    }

    private static void renderItemToTarget(ZoomedItemBufferSource bufferSource, Minecraft mc, ItemStack stack,
                                           float x, float y, int size, float scale, float rotation,
                                           int guiWidth, int guiHeight) {

        mc.getItemModelResolver().updateForTopItem(itemRenderState, stack, ItemDisplayContext.GUI, mc.level, mc.player, 0);

        PoseStack poseStack = new PoseStack();

        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;

        poseStack.translate(centerX, centerY, 150);

        if (scale != 1.0f) {
            poseStack.scale(scale, scale, 1.0f);
        }

        if (rotation != 0) {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
        }

        float itemScale = size / 16.0f;
        float baseGuiScale = 16.0f;
        poseStack.scale(baseGuiScale * itemScale, -baseGuiScale * itemScale, baseGuiScale * itemScale);

        boolean usesBlockLight = itemRenderState.usesBlockLight();
        if (usesBlockLight) {
            Lighting.setupFor3DItems();
        } else {
            Lighting.setupForFlatItems();
        }

        itemRenderState.render(poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);

        bufferSource.endBatch();

        Lighting.setupFor3DItems();

        renderItemDecorations(bufferSource, mc, stack, x, y, size, scale);
    }

    private static void renderItemDecorations(ZoomedItemBufferSource bufferSource, Minecraft mc, ItemStack stack,
                                              float x, float y, int size, float scale) {
        int count = stack.getCount();
        if (count > 1) {
            String text = String.valueOf(count);
            Font font = mc.font;

            float centerX = x + size / 2.0f;
            float centerY = y + size / 2.0f;
            float scaledSize = size * scale;

            float textX = centerX + scaledSize / 2.0f - font.width(text) - 1;
            float textY = centerY + scaledSize / 2.0f - font.lineHeight;

            PoseStack textPose = new PoseStack();
            textPose.translate(0, 0, 200);

            font.drawInBatch(text, textX, textY, 0xFFFFFFFF, true, textPose.last().pose(),
                    bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
        }
    }

    private static void renderTextToTarget(GuiGraphics graphics, Minecraft mc, ItemStack stack,
                                           float x, float y, int itemSize, float alpha) {
        if (alpha <= 0) return;

        Font font = mc.font;
        Item.TooltipContext tooltipContext = Item.TooltipContext.of(mc.level);
        List<Component> tooltip = stack.getTooltipLines(tooltipContext, mc.player, TooltipFlag.Default.NORMAL);
        if (tooltip.isEmpty()) return;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 200);

        int textAlphaInt = (int) (alpha * 255);

        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int availableHeight = (int) (screenHeight - y - 10);

        ItemEnchantments enchantments = stack.getEnchantments();
        int enchantmentCount = enchantments.size();
        int essentialLines = 1 + enchantmentCount;

        int maxWidth = Math.max(itemSize, 100);

        int totalHeight = calculateTextHeight(font, tooltip, tooltip.size(), maxWidth);
        boolean showExtras = totalHeight <= availableHeight;
        int linesToRender = showExtras ? tooltip.size() : essentialLines;

        float currentY = y;
        int sourceLineIndex = 0;

        for (int i = 0; i < tooltip.size() && sourceLineIndex < linesToRender; i++) {
            Component line = tooltip.get(i);
            String plainText = line.getString();

            if (plainText.trim().isEmpty()) {
                if (i < linesToRender) {
                    currentY += 6;
                }
                continue;
            }

            if (i >= essentialLines && !showExtras) {
                break;
            }

            int baseColor;
            float lineScale;

            if (i == 0) {
                baseColor = 0xFFFFFF;
                lineScale = 1.0f;
            } else if (i < essentialLines) {
                baseColor = 0xAAAAAA;
                lineScale = 1.0f;
            } else {
                baseColor = 0x888888;
                lineScale = 0.75f;
            }

            int color = ARGB.color(textAlphaInt, ARGB.red(baseColor), ARGB.green(baseColor), ARGB.blue(baseColor));
            int scaledMaxWidth = (int) (maxWidth / lineScale);

            List<FormattedCharSequence> wrappedLines = font.split(line, scaledMaxWidth);

            for (FormattedCharSequence wrappedLine : wrappedLines) {
                int lineWidth = font.width(wrappedLine);
                float drawX = x + (itemSize - lineWidth * lineScale) / 2;

                if (lineScale != 1.0f) {
                    pose.pushPose();
                    pose.translate(drawX, currentY, 0);
                    pose.scale(lineScale, lineScale, 1.0f);
                    graphics.drawString(font, wrappedLine, 0, 0, color, false);
                    pose.popPose();
                    currentY += (int) (10 * lineScale);
                } else {
                    graphics.drawString(font, wrappedLine, (int) drawX, (int) currentY, color, false);
                    currentY += 10;
                }
            }

            sourceLineIndex++;
        }

        pose.popPose();
        graphics.flush();
    }

    private static int calculateTextHeight(Font font, List<Component> tooltip, int maxSourceLines, int maxWidth) {
        int height = 0;

        for (int i = 0; i < Math.min(tooltip.size(), maxSourceLines); i++) {
            Component line = tooltip.get(i);
            String plainText = line.getString();

            if (plainText.trim().isEmpty()) {
                height += 6;
                continue;
            }

            float lineScale = (i == 0 || i < maxSourceLines) ? 1.0f : 0.75f;
            int scaledMaxWidth = (int) (maxWidth / lineScale);

            List<FormattedCharSequence> wrappedLines = font.split(line, scaledMaxWidth);
            height += (int) (10 * lineScale) * wrappedLines.size();
        }

        return height;
    }

    private static void ensureRenderTarget(int windowWidth, int windowHeight) {
        if (renderTarget == null || cachedWidth != windowWidth || cachedHeight != windowHeight) {
            destroyRenderTarget();
            try {
                renderTarget = new TextureTarget("ItemZoomer", windowWidth, windowHeight, true);
                cachedWidth = windowWidth;
                cachedHeight = windowHeight;
            } catch (Exception e) {
                renderTarget = null;
            }
        }
    }

    private static void destroyRenderTarget() {
        if (renderTarget != null) {
            renderTarget.destroyBuffers();
            renderTarget = null;
        }
        cachedWidth = 0;
        cachedHeight = 0;
    }

    private static void blitWithAlpha(RenderTarget source, int screenWidth, int screenHeight, float alpha, RenderTarget dest) {
        GpuTexture sourceTexture = source.getColorTexture();
        GpuTexture destTexture = dest.getColorTexture();
        if (sourceTexture == null || destTexture == null) return;

        RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indices = indexBuffer.getBuffer(6);

        int colorInt = ARGB.color((int) (alpha * 255), 255, 255, 255);

        float u = (float) source.viewWidth / source.width;
        float v = (float) source.viewHeight / source.height;

        try (ByteBufferBuilder byteBuilder = new ByteBufferBuilder(DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * 4)) {
            BufferBuilder vertexBuilder = new BufferBuilder(byteBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            vertexBuilder.addVertex(0, screenHeight, 0).setUv(0, 0).setColor(colorInt);
            vertexBuilder.addVertex(screenWidth, screenHeight, 0).setUv(u, 0).setColor(colorInt);
            vertexBuilder.addVertex(screenWidth, 0, 0).setUv(u, v).setColor(colorInt);
            vertexBuilder.addVertex(0, 0, 0).setUv(0, v).setColor(colorInt);

            MeshData meshData = vertexBuilder.buildOrThrow();

            GpuBuffer customVertices = RenderSystem.getDevice().createBuffer(
                    () -> "ItemZoomer blit vertices",
                    BufferType.VERTICES,
                    BufferUsage.DYNAMIC_WRITE,
                    meshData.vertexBuffer());

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

            try (RenderPass renderPass = encoder.createRenderPass(destTexture, OptionalInt.empty())) {
                renderPass.setPipeline(RenderPipelines.GUI_TEXTURED_OVERLAY);
                renderPass.bindSampler("Sampler0", sourceTexture);
                renderPass.setVertexBuffer(0, customVertices);
                renderPass.setIndexBuffer(indices, indexBuffer.type());

                renderPass.setUniform("ColorModulator", new float[]{1.0f, 1.0f, 1.0f, 1.0f});

                Matrix4f projMatrix = new Matrix4f().setOrtho(0, screenWidth, screenHeight, 0, -1000, 1000);
                renderPass.setUniform("ProjMat", projMatrix);

                Matrix4f modelViewMatrix = new Matrix4f().identity();
                renderPass.setUniform("ModelViewMat", modelViewMatrix);

                renderPass.drawIndexed(0, 6);
            }

            customVertices.close();
            meshData.close();
        }
    }

    private static float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    public static void cleanup() {
        destroyRenderTarget();
        animationState.reset();
        ZoomedItemBufferSource.clearCache();
    }

    public static void onScreenClose() {
        animationState.reset();
    }
}