package com.imeetake.itemzoomer.render;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.accessor.ContainerScreenAccessor;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.List;

public class ZoomedItemRenderer {

    private static final int PADDING = 5;
    private static final AnimationState animationState = new AnimationState();

    private static RenderTarget renderTarget = null;
    private static int cachedWidth = 0;
    private static int cachedHeight = 0;
    private static boolean initialized = false;

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
        int clipX = 0, clipY = 0, clipWidth = 0, clipHeight = 0;
        boolean useClipping = false;

        switch (config.appearAnimation) {
            case FADE -> alpha = eased;
            case SCALE -> scale = eased;
            case SLIDE_LEFT -> offsetX = (1 - eased) * -itemSize;
            case SLIDE_RIGHT -> {
                float distanceToGui = guiLeft - itemX + PADDING;
                offsetX = (1 - eased) * distanceToGui;
                useClipping = eased < 1.0f;
                clipX = 0;
                clipY = 0;
                clipWidth = guiLeft;
                clipHeight = mc.getWindow().getGuiScaledHeight();
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

        if (useClipping) {
            renderWithAlphaClipped(stack, finalItemX, finalItemY, itemSize, finalScale, idleAngle,
                    showInfo, finalTextX, finalTextY, true, alpha, config.infoDelaySeconds,
                    clipX, clipY, clipWidth, clipHeight);
        } else {
            renderWithAlpha(stack, finalItemX, finalItemY, itemSize, finalScale, idleAngle,
                    showInfo, finalTextX, finalTextY, true, alpha, config.infoDelaySeconds);
        }
    }

    private static boolean isLeftSideBlocked(AbstractContainerScreen<?> screen, ContainerScreenAccessor accessor) {
        if (screen instanceof RecipeUpdateListener recipeListener) {
            RecipeBookComponent recipeBook = recipeListener.getRecipeBookComponent();
            if (recipeBook != null && recipeBook.isVisible()) {
                return true;
            }
        }

        int guiLeft = accessor.itemzoomer$getLeftPos();
        int minSpace = 50;
        if (guiLeft < minSpace) {
            return true;
        }

        return false;
    }

    private static void renderWithAlpha(ItemStack stack,
                                        float itemX, float itemY, int itemSize, float scale, float rotation,
                                        boolean showInfo, float textX, float textY, boolean textBelowItem, float alpha, int infoDelaySeconds) {

        Minecraft mc = Minecraft.getInstance();
        int windowWidth = mc.getWindow().getWidth();
        int windowHeight = mc.getWindow().getHeight();
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();

        ensureRenderTarget(windowWidth, windowHeight);
        if (renderTarget == null) return;

        RenderTarget mainTarget = mc.getMainRenderTarget();

        renderTarget.clear(Minecraft.ON_OSX);
        renderTarget.bindWrite(true);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        GuiGraphics fboGraphics = new GuiGraphics(mc, bufferSource);

        renderItemToBuffer(fboGraphics, mc, stack, itemX, itemY, itemSize, scale, rotation);

        if (showInfo) {
            float textAlpha = easeOutCubic(animationState.getTextAppearProgress(infoDelaySeconds));
            renderTextToBuffer(fboGraphics, mc, stack, textX, textY, itemSize, textBelowItem, textAlpha);
        }

        fboGraphics.flush();

        mainTarget.bindWrite(true);

        blitWithAlpha(renderTarget, guiWidth, guiHeight, alpha);
    }

    private static void renderWithAlphaClipped(ItemStack stack,
                                               float itemX, float itemY, int itemSize, float scale, float rotation,
                                               boolean showInfo, float textX, float textY, boolean textBelowItem, float alpha, int infoDelaySeconds,
                                               int clipX, int clipY, int clipWidth, int clipHeight) {

        Minecraft mc = Minecraft.getInstance();
        int windowWidth = mc.getWindow().getWidth();
        int windowHeight = mc.getWindow().getHeight();
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();

        ensureRenderTarget(windowWidth, windowHeight);
        if (renderTarget == null) return;

        RenderTarget mainTarget = mc.getMainRenderTarget();

        renderTarget.clear(Minecraft.ON_OSX);
        renderTarget.bindWrite(true);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        GuiGraphics fboGraphics = new GuiGraphics(mc, bufferSource);

        renderItemToBuffer(fboGraphics, mc, stack, itemX, itemY, itemSize, scale, rotation);

        if (showInfo) {
            float textAlpha = easeOutCubic(animationState.getTextAppearProgress(infoDelaySeconds));
            renderTextToBuffer(fboGraphics, mc, stack, textX, textY, itemSize, textBelowItem, textAlpha);
        }

        fboGraphics.flush();

        mainTarget.bindWrite(true);

        blitWithAlphaClipped(renderTarget, guiWidth, guiHeight, alpha, clipX, clipY, clipWidth, clipHeight);
    }

    private static void ensureRenderTarget(int windowWidth, int windowHeight) {
        if (renderTarget == null || cachedWidth != windowWidth || cachedHeight != windowHeight) {
            destroyRenderTarget();
            try {
                renderTarget = new TextureTarget(windowWidth, windowHeight, true, Minecraft.ON_OSX);
                renderTarget.setClearColor(0, 0, 0, 0);
                cachedWidth = windowWidth;
                cachedHeight = windowHeight;
                initialized = true;
            } catch (Exception e) {
                renderTarget = null;
                initialized = false;
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

    private static void renderItemToBuffer(GuiGraphics graphics, Minecraft mc, ItemStack stack,
                                           float x, float y, int size, float scale, float rotation) {

        PoseStack pose = graphics.pose();
        pose.pushPose();

        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;

        pose.translate(centerX, centerY, 100);

        if (scale != 1.0f) {
            pose.scale(scale, scale, 1.0f);
        }

        if (rotation != 0) {
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
        }

        float itemScale = size / 16.0f;
        pose.scale(itemScale, itemScale, itemScale);
        pose.translate(-8, -8, 0);

        graphics.renderItem(stack, 0, 0);

        pose.popPose();

        renderItemCount(graphics, mc, stack, x, y, size);
    }

    private static void renderItemCount(GuiGraphics graphics, Minecraft mc, ItemStack stack, float x, float y, int size) {
        int count = stack.getCount();
        if (count <= 1) return;

        String text = "x" + count;
        Font font = mc.font;

        float textX = x + size - font.width(text);
        float textY = y + size - font.lineHeight;

        graphics.drawString(font, text, (int) textX, (int) textY, 0xFFFFFFFF, true);
    }

    private static void renderTextToBuffer(GuiGraphics graphics, Minecraft mc, ItemStack stack,
                                           float x, float y, int itemSize, boolean centerHorizontally, float alpha) {

        Font font = mc.font;
        Item.TooltipContext tooltipContext = Item.TooltipContext.of(mc.level);
        List<Component> tooltip = stack.getTooltipLines(tooltipContext, mc.player, TooltipFlag.Default.NORMAL);
        if (tooltip.isEmpty()) return;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 200);

        int textAlpha = (int) (alpha * 255);
        if (textAlpha <= 0) {
            pose.popPose();
            return;
        }

        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int availableHeight = (int) (screenHeight - y - 10);

        ItemEnchantments enchantments = stack.getEnchantments();
        int enchantmentCount = enchantments.size();
        int essentialLines = 1 + enchantmentCount;

        int maxWidth = Math.max(itemSize, 100);

        int essentialHeight = calculateTextHeight(font, tooltip, essentialLines, maxWidth);
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

            int color = (textAlpha << 24) | baseColor;
            int scaledMaxWidth = (int) (maxWidth / lineScale);

            List<FormattedCharSequence> wrappedLines = font.split(line, scaledMaxWidth);

            for (FormattedCharSequence wrappedLine : wrappedLines) {
                int lineWidth = font.width(wrappedLine);
                float textX = x;
                if (centerHorizontally) {
                    textX = x + (itemSize - lineWidth * lineScale) / 2;
                }

                if (lineScale != 1.0f) {
                    pose.pushPose();
                    pose.translate(textX, currentY, 0);
                    pose.scale(lineScale, lineScale, 1.0f);
                    graphics.drawString(font, wrappedLine, 0, 0, color, false);
                    pose.popPose();
                    currentY += (int) (10 * lineScale);
                } else {
                    graphics.drawString(font, wrappedLine, (int) textX, (int) currentY, color, false);
                    currentY += 10;
                }
            }

            sourceLineIndex++;
        }

        pose.popPose();
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

    private static void blitWithAlpha(RenderTarget source, int screenWidth, int screenHeight, float alpha) {
        Matrix4f savedProjection = RenderSystem.getProjectionMatrix();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, source.getColorTextureId());

        Matrix4f matrix = new Matrix4f().setOrtho(0, screenWidth, screenHeight, 0, 1000, 3000);
        RenderSystem.setProjectionMatrix(matrix, VertexSorting.ORTHOGRAPHIC_Z);

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        modelView.translate(0, 0, -2000);
        RenderSystem.applyModelViewMatrix();

        float u = (float) source.viewWidth / source.width;
        float v = (float) source.viewHeight / source.height;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        buffer.addVertex(0, 0, 0).setUv(0, v).setColor(1f, 1f, 1f, alpha);
        buffer.addVertex(0, screenHeight, 0).setUv(0, 0).setColor(1f, 1f, 1f, alpha);
        buffer.addVertex(screenWidth, screenHeight, 0).setUv(u, 0).setColor(1f, 1f, 1f, alpha);
        buffer.addVertex(screenWidth, 0, 0).setUv(u, v).setColor(1f, 1f, 1f, alpha);

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.setProjectionMatrix(savedProjection, VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void blitWithAlphaClipped(RenderTarget source, int screenWidth, int screenHeight, float alpha,
                                             int clipX, int clipY, int clipWidth, int clipHeight) {
        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();

        int scissorX = (int) (clipX * guiScale);
        int scissorY = (int) ((screenHeight - clipY - clipHeight) * guiScale);
        int scissorW = (int) (clipWidth * guiScale);
        int scissorH = (int) (clipHeight * guiScale);

        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

        blitWithAlpha(source, screenWidth, screenHeight, alpha);

        RenderSystem.disableScissor();
    }

    private static float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    public static void cleanup() {
        destroyRenderTarget();
        animationState.reset();
        initialized = false;
    }

    public static void onScreenClose() {
        animationState.reset();
    }

    public static boolean isInitialized() {
        return initialized;
    }
}