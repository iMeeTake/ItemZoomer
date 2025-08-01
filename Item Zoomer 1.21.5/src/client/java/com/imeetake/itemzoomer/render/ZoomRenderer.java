package com.imeetake.itemzoomer.render;

import com.imeetake.itemzoomer.mixin.client.HandledScreenAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.joml.Vector3f;

public class ZoomRenderer {

    /** Рисуем увеличенную иконку предмета под курсором */
    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;

        // координаты курсора с учётом масштабирования
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth()  / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();

        int guiLeft = ((HandledScreenAccessor) screen).getX();
        int guiTop  = ((HandledScreenAccessor) screen).getY();

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        /* ---------- размеры и позиционирование ---------- */
        int size = (int) (Math.min(sw, sh) * 0.45);           // 45 % меньшей стороны
        int leftSpace = guiLeft;                              // свободное поле слева от GUI

        // если предмет помещается — центрируем по полю, иначе прижимаем к GUI
        int x = (size + 8 <= leftSpace) ? (leftSpace - size) / 2
                : guiLeft - size - 4;

        int y = (sh - size) / 2;                              // по вертикали всегда центр

        /* ---------- ищем слот под курсором ---------- */
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

    private static boolean isMouseOverSlot(Slot slot, double mx, double my, int left, int top) {
        int sx = left + slot.x;
        int sy = top  + slot.y;
        return mx >= sx && mx <= sx + 16 && my >= sy && my <= sy + 16;
    }

    /** Рендер увеличенной иконки */
    private static void renderZoomedItem(DrawContext ctx, ItemStack stack, int x, int y, int size) {
        MinecraftClient client = MinecraftClient.getInstance();
        var matrices  = ctx.getMatrices();
        var renderer  = client.getItemRenderer();
        VertexConsumerProvider.Immediate consumers =
                client.getBufferBuilders().getEntityVertexConsumers();

        /* ---------- освещение ---------- */
        boolean isBlockItem = stack.getItem() instanceof BlockItem;
        if (isBlockItem) {   // блоки
            RenderSystem.setupGui3DDiffuseLighting(
                    new Vector3f(0.2f, 1.0f, -0.6f),
                    new Vector3f(-0.2f, 1.0f, 0.6f));
        } else {             // плоские предметы
            RenderSystem.setupGuiFlatDiffuseLighting(
                    new Vector3f(0.2f, 1.0f, -0.6f),
                    new Vector3f(-0.2f, 1.0f, 0.6f));
        }

        /* ---------- матрицы и сам рендер ---------- */
        matrices.push();
        matrices.translate(x + size / 2f, y + size / 2f, 200);
        matrices.scale(size, -size, size);

        renderer.renderItem(
                stack,
                ItemDisplayContext.GUI,
                0xF000F0,                              // максимальный свет
                OverlayTexture.DEFAULT_UV,
                matrices,
                consumers,
                client.world,
                0
        );

        matrices.pop();
        consumers.draw();                              // сбрасываем буферы в GPU

        /* ---------- возвращаем стандартный GUI‑свет ---------- */
        RenderSystem.setupGuiFlatDiffuseLighting(
                new Vector3f(0.2f, 1.0f, -0.6f),
                new Vector3f(-0.2f, 1.0f, 0.6f));
    }
}
