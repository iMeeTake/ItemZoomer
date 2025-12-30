package com.imeetake.itemzoomer.render;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.accessor.ContainerScreenAccessor;
import com.imeetake.itemzoomer.accessor.RecipeBookScreenAccessor;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.imeetake.itemzoomer.mixin.GuiGraphicsAccessor;
import com.imeetake.itemzoomer.mixin.GuiRenderStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;

public class ZoomedItemRenderer {

    private static final int PADDING = 5;
    private static final AnimationState animationState = new AnimationState();
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
        ScreenRectangle scissor = null;

        switch (config.appearAnimation) {
            case FADE -> alpha = eased;
            case SCALE -> scale = eased;
            case SLIDE_LEFT -> offsetX = (1 - eased) * -itemSize;
            case SLIDE_RIGHT -> {
                float distanceToGui = guiLeft - itemX + PADDING;
                offsetX = (1 - eased) * distanceToGui;
                scissor = new ScreenRectangle(0, 0, guiLeft, mc.getWindow().getGuiScaledHeight());
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
        float scaledSize = itemSize * finalScale;
        float finalItemX = itemX + offsetX + (itemSize - scaledSize) / 2.0f;
        float finalItemY = itemY + offsetY + (itemSize - scaledSize) / 2.0f;
        float finalTextX = textX + offsetX;
        float finalTextY = textY + offsetY;

        boolean showInfo = config.showItemInfo && animationState.shouldShowInfo(config.infoDelaySeconds);
        float textAlpha = showInfo ? easeOutCubic(animationState.getTextAppearProgress(config.infoDelaySeconds)) : 0;

        mc.getItemModelResolver().updateForTopItem(itemRenderState, stack, ItemDisplayContext.GUI, mc.level, mc.player, 0);

        GuiRenderState guiRenderState = ((GuiGraphicsAccessor) graphics).itemzoomer$getGuiRenderState();

        float x0 = finalItemX;
        float y0 = finalItemY;
        float x1 = finalItemX + scaledSize;
        float y1 = finalItemY + scaledSize;

        ZoomedItemRenderState state = new ZoomedItemRenderState(
                itemRenderState,
                x0, y0, x1, y1,
                idleAngle,
                alpha,
                scissor
        );

        ((GuiRenderStateAccessor) guiRenderState).itemzoomer$submitPictureInPicture(state);

        if (showInfo && textAlpha > 0) {
            renderItemInfo(graphics, mc, stack, finalTextX, finalTextY, itemSize, textAlpha * alpha);
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

    private static void renderItemInfo(GuiGraphics graphics, Minecraft mc, ItemStack stack,
                                       float x, float y, int itemSize, float alpha) {
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

    public static void cleanup() {
        animationState.reset();
    }

    public static void onScreenClose() {
        animationState.reset();
    }
}