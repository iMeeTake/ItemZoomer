package com.imeetake.itemzoomer.render;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.accessor.ContainerScreenAccessor;
import com.imeetake.itemzoomer.compat.HoveredStackProviderRegistry;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ZoomedItemRenderer {

    private static final int PADDING = 5;
    private static final int BASE_SIZE = 115;
    private static final float BACKING_SCREEN_SIZE_SCALE = 0.95f;
    private static final AnimationState animationState = new AnimationState();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> LOGGED_RENDER_FAILURES = ConcurrentHashMap.newKeySet();

    private static RenderTarget renderTarget = null;
    private static int cachedWidth = 0;
    private static int cachedHeight = 0;
    private static boolean initialized = false;
    private static boolean renderedThisFrame = false;

    public static void beginFrame() {
        animationState.beginFrame();
        renderedThisFrame = false;
    }

    public static void render(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
        if (shouldDeferToViewer()) {
            return;
        }
        doRender(graphics, screen, mouseX, mouseY);
    }

    public static void renderFromViewer(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
        doRender(graphics, screen, mouseX, mouseY);
    }

    private static boolean shouldDeferToViewer() {
        if (ItemZoomerConfig.get().favoritesOverlap != ItemZoomerConfig.FavoritesOverlap.ABOVE) {
            return false;
        }
        return HoveredStackProviderRegistry.shouldDeferAbove();
    }

    private static void doRender(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
        if (!ItemZoomer.isEnabled()) {
            animationState.reset();
            return;
        }

        if (renderedThisFrame) {
            return;
        }
        renderedThisFrame = true;

        ItemZoomerConfig config = ItemZoomerConfig.get();
        ContainerScreenAccessor accessor = screen instanceof ContainerScreenAccessor currentAccessor ? currentAccessor : null;
        AbstractContainerScreen<?> containerScreen = screen instanceof AbstractContainerScreen<?> currentScreen ? currentScreen : null;

        ItemStack liveStack = null;

        if (accessor != null) {
            Slot hoveredSlot = accessor.itemzoomer$getHoveredSlot();
            if (hoveredSlot != null && hoveredSlot.hasItem()) {
                liveStack = hoveredSlot.getItem();
            }
        }

        if (liveStack == null || liveStack.isEmpty()) {
            ItemStack compatStack = HoveredStackProviderRegistry.getHoveredStack(mouseX, mouseY);
            if (compatStack != null && !compatStack.isEmpty()) {
                liveStack = compatStack;
            }
        }

        if (liveStack != null && !liveStack.isEmpty()
                && containerScreen != null && accessor != null
                && isLeftSideBlocked(containerScreen, accessor)) {
            liveStack = null;
        }

        Minecraft mc = Minecraft.getInstance();
        boolean hasAppearAnimation = config.appearAnimation != ItemZoomerConfig.AppearAnimation.NONE;
        Object renderIdentity = null;
        if (liveStack != null && !liveStack.isEmpty()) {
            BakedModel renderModel = mc.getItemRenderer().getModel(liveStack, mc.level, mc.player, 0);
            renderIdentity = renderModel != null ? renderModel : liveStack.getItem();
        }

        animationState.update(liveStack, renderIdentity, hasAppearAnimation, config.appearDelayMs, config.infoDelaySeconds);

        ItemStack stack = animationState.getRenderStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }

        Rect2i windowBounds = null;
        boolean fromBackingScreen = false;
        if (accessor == null) {
            windowBounds = HoveredStackProviderRegistry.getWindowBounds();
            if (windowBounds == null) {
                AbstractContainerScreen<?> backingScreen = HoveredStackProviderRegistry.getBackingScreen();
                if (backingScreen instanceof ContainerScreenAccessor backingAccessor) {
                    accessor = backingAccessor;
                    fromBackingScreen = true;
                }
            }
        }

        graphics.flush();

        try {
            if (accessor != null) {
                float sizeScale = fromBackingScreen ? BACKING_SCREEN_SIZE_SCALE : 1.0f;
                renderZoomedItem(graphics, accessor, stack, config, sizeScale);
            } else if (windowBounds != null) {
                renderZoomedItem(graphics, windowBounds.getX(), windowBounds.getY(), windowBounds.getHeight(), stack, config, 1.0f);
            } else {
                int fallbackLeft = Math.min(screen.width / 2, 2 * PADDING + BASE_SIZE);
                renderZoomedItem(graphics, fallbackLeft, 0, screen.height, stack, config, 1.0f);
            }
        } catch (Throwable t) {
            logRenderFailure(stack.getItem(), t);
        }
    }

    private static void renderZoomedItem(GuiGraphics graphics, ContainerScreenAccessor accessor, ItemStack stack, ItemZoomerConfig config, float sizeScale) {
        renderZoomedItem(graphics, accessor.itemzoomer$getLeftPos(), accessor.itemzoomer$getTopPos(), accessor.itemzoomer$getImageHeight(), stack, config, sizeScale);
    }

    private static void renderZoomedItem(GuiGraphics graphics, int guiLeft, int guiTop, int guiHeight, ItemStack stack, ItemZoomerConfig config, float sizeScale) {
        Minecraft mc = Minecraft.getInstance();

        float progress = animationState.getAppearProgress();
        if (progress <= 0) return;

        int availableWidth = Math.max(0, guiLeft - PADDING * 2);
        int maxItemSize = availableWidth;
        if (maxItemSize < 32) return;

        float sizeMultiplier = clamp(config.itemSizePercent, 50, 150) / 100.0f;
        int itemSize = (int) (Math.min(BASE_SIZE, availableWidth) * sizeMultiplier * sizeScale);
        itemSize = Math.max(32, Math.min(itemSize, maxItemSize));

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

        float idleTime = animationState.getIdleAnimationTime();
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

        boolean showInfo = config.showItemInfo && animationState.shouldShowInfo();

        if (config.favoritesOverlap == ItemZoomerConfig.FavoritesOverlap.HIDE
                && overlapsExclusion((int) finalItemX, (int) finalItemY, itemSize, itemSize)) {
            return;
        }

        renderOffscreen(graphics, stack, finalItemX, finalItemY, itemSize, finalScale, idleAngle,
                showInfo, finalTextX, finalTextY, alpha,
                useClipping, clipX, clipY, clipWidth, clipHeight);
    }

    private static boolean overlapsExclusion(int x, int y, int width, int height) {
        for (Rect2i b : HoveredStackProviderRegistry.getExclusionBounds()) {
            if (b == null || b.getWidth() <= 0 || b.getHeight() <= 0) continue;
            if (x < b.getX() + b.getWidth() && x + width > b.getX()
                    && y < b.getY() + b.getHeight() && y + height > b.getY()) {
                return true;
            }
        }
        return false;
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

    private static void renderOffscreen(GuiGraphics graphics, ItemStack stack,
                                        float itemX, float itemY, int itemSize, float scale, float rotation,
                                        boolean showInfo, float textX, float textY, float alpha,
                                        boolean useClipping, int clipX, int clipY, int clipWidth, int clipHeight) {

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

        try {
            GuiGraphics fboGraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());

            renderItemContent(fboGraphics, mc, stack, itemX, itemY, itemSize, scale, rotation);

            if (showInfo) {
                float textAlpha = easeOutCubic(animationState.getTextProgress());
                renderTextContent(fboGraphics, mc, stack, textX, textY, itemSize, true, textAlpha);
            }

            fboGraphics.flush();
        } finally {
            mainTarget.bindWrite(true);
        }

        if (useClipping) {
            blitWithAlphaClipped(renderTarget, guiWidth, guiHeight, alpha, clipX, clipY, clipWidth, clipHeight);
        } else {
            blitWithAlpha(renderTarget, guiWidth, guiHeight, alpha);
        }
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

    private static void renderItemContent(GuiGraphics graphics, Minecraft mc, ItemStack stack,
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

    private static void renderTextContent(GuiGraphics graphics, Minecraft mc, ItemStack stack,
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static void logRenderFailure(Item item, Throwable t) {
        String id = item != null ? BuiltInRegistries.ITEM.getKey(item).toString() : "unknown item";
        if (LOGGED_RENDER_FAILURES.add(id)) {
            LOGGER.warn("Item Zoomer: skipping the zoom overlay for {} because it threw while rendering (most likely a bug in that item's own mod, not Item Zoomer). Further failures for this item are silenced.", id, t);
        }
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
