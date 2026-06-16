package com.imeetake.itemzoomer.render;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.accessor.ContainerScreenAccessor;
import com.imeetake.itemzoomer.accessor.RecipeBookScreenAccessor;
import com.imeetake.itemzoomer.compat.HoveredStackProviderRegistry;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.imeetake.itemzoomer.mixin.GuiGraphicsAccessor;
import com.imeetake.itemzoomer.mixin.GuiRenderStateAccessor;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ZoomedItemRenderer {

    private static final int PADDING = 5;
    private static final int BASE_SIZE = 115;
    private static final float BACKING_SCREEN_SIZE_SCALE = 0.95f;
    private static final float MAX_IDLE_SCALE = 1.02f;
    private static final ModelFootprint NORMAL_MODEL_FOOTPRINT = new ModelFootprint(-0.5f, -0.5f, 0.5f, 0.5f);
    private static final AnimationState animationState = new AnimationState();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> LOGGED_RENDER_FAILURES = ConcurrentHashMap.newKeySet();
    private static boolean renderedThisFrame = false;
    private static Item currentZoomItem = null;

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

    private static void doRender(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
        try {
            doRender0(graphics, screen, mouseX, mouseY);
        } catch (Throwable t) {
            logCurrentRenderFailure(t);
        }
    }

    private static void doRender0(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
        if (!ItemZoomer.isEnabled()) {
            animationState.reset();
            return;
        }

        if (renderedThisFrame) {
            return;
        }
        renderedThisFrame = true;
        currentZoomItem = null;

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

        TrackingItemStackRenderState itemRenderState = null;
        Object renderIdentity = null;
        if (liveStack != null && !liveStack.isEmpty()) {
            itemRenderState = new TrackingItemStackRenderState();
            mc.getItemModelResolver().updateForTopItem(itemRenderState, liveStack, ItemDisplayContext.GUI, mc.level, mc.player, 0);
            renderIdentity = itemRenderState.getModelIdentity();
        }

        animationState.update(liveStack, renderIdentity, hasAppearAnimation, config.appearDelayMs, config.infoDelaySeconds);

        ItemStack stack = animationState.getRenderStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        currentZoomItem = stack.getItem();

        if (itemRenderState == null) {
            itemRenderState = new TrackingItemStackRenderState();
            mc.getItemModelResolver().updateForTopItem(itemRenderState, stack, ItemDisplayContext.GUI, mc.level, mc.player, 0);
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

        if (accessor != null) {
            float sizeScale = fromBackingScreen ? BACKING_SCREEN_SIZE_SCALE : 1.0f;
            renderZoomedItem(graphics, accessor, stack, config, itemRenderState, sizeScale);
        } else if (windowBounds != null) {
            renderZoomedItem(graphics, windowBounds.getX(), windowBounds.getY(), windowBounds.getHeight(), stack, config, itemRenderState, 1.0f);
        } else {
            int fallbackLeft = Math.min(screen.width / 2, 2 * PADDING + BASE_SIZE);
            renderZoomedItem(graphics, fallbackLeft, 0, screen.height, stack, config, itemRenderState, 1.0f);
        }
    }

    private static void renderZoomedItem(GuiGraphics graphics, ContainerScreenAccessor accessor, ItemStack stack, ItemZoomerConfig config, ItemStackRenderState itemRenderState, float sizeScale) {
        renderZoomedItem(graphics, accessor.itemzoomer$getLeftPos(), accessor.itemzoomer$getTopPos(), accessor.itemzoomer$getImageHeight(), stack, config, itemRenderState, sizeScale);
    }

    private static void renderZoomedItem(GuiGraphics graphics, int guiLeft, int guiTop, int guiHeight, ItemStack stack, ItemZoomerConfig config, ItemStackRenderState itemRenderState, float sizeScale) {
        Minecraft mc = Minecraft.getInstance();

        float progress = animationState.getAppearProgress();
        if (progress <= 0) return;

        int availableWidth = Math.max(0, guiLeft - PADDING * 2);
        ModelFootprint modelFootprint = getModelFootprint(itemRenderState);
        ModelFootprint layoutFootprint = modelFootprint.expandedForRotation(config.idleAnimation == ItemZoomerConfig.IdleAnimation.SWING ? 2.0f : 0.0f);
        float horizontalFootprint = layoutFootprint.horizontalFootprint();
        float verticalFootprint = layoutFootprint.verticalFootprint();
        float maxItemSize = availableWidth / horizontalFootprint;
        float minItemSize = 32.0f / horizontalFootprint;
        if (maxItemSize < minItemSize || maxItemSize < 1.0f) return;

        float sizeMultiplier = clamp(config.itemSizePercent, 50, 150) / 100.0f;
        float desiredItemSize = Math.min(BASE_SIZE, maxItemSize) * sizeMultiplier * sizeScale;
        int itemSize = Math.max(1, Math.round(Math.max(minItemSize, Math.min(desiredItemSize, maxItemSize))));

        float centerX = guiLeft / 2.0f;
        float centerY = guiTop + (guiHeight / 2.0f);

        float visualWidth = itemSize * horizontalFootprint;
        float visualHeight = itemSize * verticalFootprint;
        float itemX = centerX - (visualWidth / 2.0f);
        float itemY = centerY - (visualHeight / 2.0f);
        float textX = itemX;
        int textGap = 12;
        float textY = itemY + visualHeight + textGap;

        float eased = easeOutCubic(progress);
        float alpha = 1.0f;
        float offsetX = 0, offsetY = 0;
        float scale = 1.0f;
        ScreenRectangle scissor = null;

        switch (config.appearAnimation) {
            case FADE -> alpha = eased;
            case SCALE -> scale = eased;
            case SLIDE_LEFT -> offsetX = (1 - eased) * -visualWidth;
            case SLIDE_RIGHT -> {
                float distanceToGui = guiLeft - itemX + PADDING;
                offsetX = (1 - eased) * distanceToGui;
                scissor = new ScreenRectangle(0, 0, guiLeft, mc.getWindow().getGuiScaledHeight());
            }
            case SLIDE_TOP -> offsetY = (1 - eased) * -visualHeight;
            case SLIDE_BOTTOM -> offsetY = (1 - eased) * visualHeight;
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
        float textureScale = MAX_IDLE_SCALE;
        float modelScale = itemSize * textureScale;
        float contentScale = finalScale / textureScale;
        float slotCenterX = centerX + offsetX;
        float slotCenterY = centerY + offsetY;
        ModelFootprint renderFootprint = modelFootprint.rotated(idleAngle);
        float finalTextX = textX + offsetX;
        float finalTextY = textY + offsetY;

        boolean showInfo = config.showItemInfo && animationState.shouldShowInfo();
        float textAlpha = showInfo ? easeOutCubic(animationState.getTextProgress()) : 0;

        if (config.favoritesOverlap == ItemZoomerConfig.FavoritesOverlap.HIDE
                && overlapsExclusion((int) (itemX + offsetX), (int) (itemY + offsetY),
                        (int) Math.ceil(visualWidth), (int) Math.ceil(visualHeight))) {
            return;
        }

        GuiRenderState guiRenderState = ((GuiGraphicsAccessor) graphics).itemzoomer$getGuiRenderState();

        float x0 = slotCenterX + renderFootprint.minX() * modelScale;
        float y0 = slotCenterY - renderFootprint.maxY() * modelScale;
        float x1 = slotCenterX + renderFootprint.maxX() * modelScale;
        float y1 = slotCenterY - renderFootprint.minY() * modelScale;

        ZoomedItemRenderState state = new ZoomedItemRenderState(
                itemRenderState,
                x0, y0, x1, y1,
                modelScale,
                -renderFootprint.centerX(),
                -renderFootprint.centerY(),
                contentScale,
                idleAngle,
                alpha,
                scissor
        );

        ((GuiRenderStateAccessor) guiRenderState).itemzoomer$submitPictureInPicture(state);

        renderItemCount(graphics, mc, stack, itemX + offsetX, itemY + offsetY, visualWidth, visualHeight, alpha);

        if (showInfo && textAlpha > 0) {
            renderItemInfo(graphics, mc, stack, finalTextX, finalTextY, (int) Math.ceil(visualWidth), textAlpha * alpha);
        }
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

    private static ModelFootprint getModelFootprint(ItemStackRenderState itemRenderState) {
        if (!itemRenderState.isOversizedInGui()) {
            return NORMAL_MODEL_FOOTPRINT;
        }

        AABB bounds = itemRenderState.getModelBoundingBox();
        if (bounds == null) {
            return NORMAL_MODEL_FOOTPRINT;
        }

        float minX = (float) bounds.minX;
        float minY = (float) bounds.minY;
        float maxX = (float) bounds.maxX;
        float maxY = (float) bounds.maxY;

        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(maxX) || !Float.isFinite(maxY)) {
            return NORMAL_MODEL_FOOTPRINT;
        }

        if (maxX <= minX || maxY <= minY) {
            return NORMAL_MODEL_FOOTPRINT;
        }

        if (maxX - minX <= 1.0f && maxY - minY <= 1.0f) {
            return NORMAL_MODEL_FOOTPRINT;
        }

        return new ModelFootprint(minX, minY, maxX, maxY);
    }

    private static void renderItemCount(GuiGraphics graphics, Minecraft mc, ItemStack stack, float x, float y, float width, float height, float alpha) {
        int count = stack.getCount();
        if (count <= 1 || alpha <= 0) return;

        String text = "x" + count;
        Font font = mc.font;

        float textX = x + width - font.width(text);
        float textY = y + height - font.lineHeight;
        int textAlpha = Math.min(255, Math.max(0, (int) (alpha * 255)));
        int color = ARGB.color(textAlpha, 255, 255, 255);

        graphics.drawString(font, text, (int) textX, (int) textY, color, true);
    }

    private static void renderItemInfo(GuiGraphics graphics, Minecraft mc, ItemStack stack,
                                       float x, float y, int itemInfoWidth, float alpha) {
        if (alpha <= 0) return;

        Font font = mc.font;
        Item.TooltipContext tooltipContext = Item.TooltipContext.of(mc.level);
        List<Component> tooltip = stack.getTooltipLines(tooltipContext, mc.player, TooltipFlag.Default.NORMAL);
        if (tooltip.isEmpty()) return;

        int textAlphaInt = (int) (alpha * 255);

        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int availableHeight = (int) (screenHeight - y - 10);

        ItemEnchantments enchantments = stack.getEnchantments();
        int enchantmentCount = enchantments.size();
        int essentialLines = 1 + enchantmentCount;

        int maxWidth = Math.max(itemInfoWidth, 100);

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
                float drawX = x + (itemInfoWidth - lineWidth * lineScale) / 2;

                if (lineScale != 1.0f) {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(drawX, currentY);
                    graphics.pose().scale(lineScale, lineScale);
                    graphics.drawString(font, wrappedLine, 0, 0, color, false);
                    graphics.pose().popMatrix();
                    currentY += (int) (10 * lineScale);
                } else {
                    graphics.drawString(font, wrappedLine, (int) drawX, (int) currentY, color, false);
                    currentY += 10;
                }
            }

            sourceLineIndex++;
        }
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

    private static float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ModelFootprint(float minX, float minY, float maxX, float maxY) {
        private float horizontalFootprint() {
            return Math.max(1.0f, Math.max(Math.abs(minX), Math.abs(maxX)) * 2.0f);
        }

        private float verticalFootprint() {
            return Math.max(1.0f, Math.max(Math.abs(minY), Math.abs(maxY)) * 2.0f);
        }

        private float centerX() {
            return (minX + maxX) / 2.0f;
        }

        private float centerY() {
            return (minY + maxY) / 2.0f;
        }

        private ModelFootprint rotated(float degrees) {
            if (degrees == 0.0f) {
                return this;
            }

            float radians = (float) Math.toRadians(degrees);
            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);

            float x1 = minX * cos - minY * sin;
            float y1 = minX * sin + minY * cos;
            float x2 = minX * cos - maxY * sin;
            float y2 = minX * sin + maxY * cos;
            float x3 = maxX * cos - minY * sin;
            float y3 = maxX * sin + minY * cos;
            float x4 = maxX * cos - maxY * sin;
            float y4 = maxX * sin + maxY * cos;

            float rotatedMinX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
            float rotatedMinY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
            float rotatedMaxX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
            float rotatedMaxY = Math.max(Math.max(y1, y2), Math.max(y3, y4));

            return new ModelFootprint(rotatedMinX, rotatedMinY, rotatedMaxX, rotatedMaxY);
        }

        private ModelFootprint expandedForRotation(float degrees) {
            if (degrees == 0.0f) {
                return this;
            }

            return rotated(degrees).union(rotated(-degrees));
        }

        private ModelFootprint union(ModelFootprint other) {
            return new ModelFootprint(
                    Math.min(minX, other.minX),
                    Math.min(minY, other.minY),
                    Math.max(maxX, other.maxX),
                    Math.max(maxY, other.maxY)
            );
        }
    }

    static void logCurrentRenderFailure(Throwable t) {
        logRenderFailure(currentZoomItem, t);
    }

    static void logRenderFailure(Item item, Throwable t) {
        String id = item != null ? BuiltInRegistries.ITEM.getKey(item).toString() : "unknown item";
        if (LOGGED_RENDER_FAILURES.add(id)) {
            LOGGER.warn("Item Zoomer: skipping the zoom overlay for {} because it threw while rendering (most likely a bug in that item's own mod, not Item Zoomer). Further failures for this item are silenced.", id, t);
        }
    }

    public static void cleanup() {
        animationState.reset();
    }

    public static void onScreenClose() {
        animationState.reset();
    }
}
