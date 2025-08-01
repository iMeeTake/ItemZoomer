package com.imeetake.itemzoomer.render;

import com.imeetake.itemzoomer.mixin.client.HandledScreenAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.joml.Vector3f;

public class ZoomRenderer {

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;

        // Получаем координаты мыши с учётом масштабирования
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();

        // Получаем координаты GUI через Accessor
        int guiLeft = ((HandledScreenAccessor) screen).getX();
        int guiTop = ((HandledScreenAccessor) screen).getY();

        int scaledWidth = client.getWindow().getScaledWidth();
        int scaledHeight = client.getWindow().getScaledHeight();

        // Размер зума — 45% от меньшей стороны экрана
        int size = (int) (Math.min(scaledWidth, scaledHeight) * 0.45);

        // Центрируем слева от GUI
        int x = (guiLeft - size) / 2;
        int y = (scaledHeight / 2) - (size / 2);

        for (Slot slot : screen.getScreenHandler().slots) {
            if (isMouseOverSlot(slot, mouseX, mouseY, guiLeft, guiTop)) {
                ItemStack stack = slot.getStack();
                if (!stack.isEmpty()) {
                    renderZoomedItem(context, stack, x, y, size);
                }
                break;
            }
        }
    }

    private static boolean isMouseOverSlot(Slot slot, double mouseX, double mouseY, int guiLeft, int guiTop) {
        int slotX = guiLeft + slot.x;
        int slotY = guiTop + slot.y;
        return mouseX >= slotX && mouseX <= slotX + 16 &&
                mouseY >= slotY && mouseY <= slotY + 16;
    }

    private static void renderZoomedItem(DrawContext context, ItemStack stack, int x, int y, int size) {
        MinecraftClient client = MinecraftClient.getInstance();
        var matrices = context.getMatrices();
        var vertexConsumers = context.getVertexConsumers();
        var renderer = client.getItemRenderer();

        var model = renderer.getModel(stack, client.world, client.player, 0);
        boolean is3D = model.hasDepth();

        matrices.push();

        // Освещение
        if (is3D) {
            RenderSystem.setupGui3DDiffuseLighting(
                    new Vector3f(0.2f, 1.0f, -0.6f),
                    new Vector3f(-0.2f, 1.0f, 0.6f)
            );
        } else {
            RenderSystem.setupGuiFlatDiffuseLighting(
                    new Vector3f(0.2f, 1.0f, -0.6f),
                    new Vector3f(-0.2f, 1.0f, 0.6f)
            );
        }

        // Центрирование и масштаб
        matrices.translate(x + size / 2f, y + size / 2f, 200);
        matrices.scale(size, -size, size);

        // Рендер предмета с полной яркостью
        renderer.renderItem(
                stack,
                ModelTransformationMode.GUI,
                false,
                matrices,
                vertexConsumers,
                0xF000F0,
                OverlayTexture.DEFAULT_UV,
                model
        );

        matrices.pop();

        // Возвращаем стандартное освещение GUI
        RenderSystem.setupGuiFlatDiffuseLighting(
                new Vector3f(0.2f, 1.0f, -0.6f),
                new Vector3f(-0.2f, 1.0f, 0.6f)
        );
    }
}
