package com.imeetake.itemzoomer.compat;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EmiHoveredStackHelper {

    public static List<Rect2i> getExclusionBounds() {
        List<Rect2i> result = new ArrayList<>();
        try {
            if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) {
                return result;
            }

            Class<?> managerClass = Class.forName("dev.emi.emi.screen.EmiScreenManager");
            Field panelsField = managerClass.getDeclaredField("panels");
            panelsField.setAccessible(true);
            Object value = panelsField.get(null);
            if (!(value instanceof List<?> panels)) {
                return result;
            }

            for (Object panel : panels) {
                if (panel == null) continue;
                try {
                    if (!isPanelVisible(panel)) continue;

                    Method boundsMethod = panel.getClass().getDeclaredMethod("getBounds");
                    boundsMethod.setAccessible(true);
                    Object bounds = boundsMethod.invoke(panel);
                    if (bounds == null) continue;

                    int x = invokeInt(bounds, "x");
                    int y = invokeInt(bounds, "y");
                    int width = invokeInt(bounds, "width");
                    int height = invokeInt(bounds, "height");
                    if (width <= 0 || height <= 0) continue;

                    int occupiedHeight = occupiedHeight(panel, height);
                    if (occupiedHeight == 0) continue;

                    result.add(new Rect2i(x, y, width, occupiedHeight > 0 ? occupiedHeight : height));
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    private static boolean isPanelVisible(Object panel) {
        try {
            Method isVisible = panel.getClass().getDeclaredMethod("isVisible");
            isVisible.setAccessible(true);
            Object visible = isVisible.invoke(panel);
            return !Boolean.FALSE.equals(visible);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static int occupiedHeight(Object panel, int fullHeight) {
        try {
            Field spaceField = panel.getClass().getDeclaredField("space");
            spaceField.setAccessible(true);
            Object space = spaceField.get(panel);
            if (space == null) return -1;

            int cols = space.getClass().getField("tw").getInt(space);
            int gridRows = space.getClass().getField("th").getInt(space);
            if (cols <= 0 || gridRows <= 0) return -1;

            Object stacks = space.getClass().getMethod("getStacks").invoke(space);
            int count = (stacks instanceof List<?> list) ? list.size() : 0;
            if (count <= 0) return 0;

            int rows = Math.min((count + cols - 1) / cols, gridRows);
            int chrome = Math.max(0, fullHeight - gridRows * 18);
            return rows * 18 + chrome;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int invokeInt(Object target, String methodName) throws ReflectiveOperationException {
        Object value = target.getClass().getMethod(methodName).invoke(target);
        return ((Number) value).intValue();
    }

    @Nullable
    public static ItemStack getStack(double mouseX, double mouseY) {
        try {
            Object screen = Minecraft.getInstance().screen;
            if (!isSupportedScreen(screen)) {
                return null;
            }

            ItemStack recipeScreenStack = getRecipeScreenStack(screen);
            if (recipeScreenStack != null && !recipeScreenStack.isEmpty()) {
                return recipeScreenStack;
            }

            ItemStack bomScreenStack = getBomScreenStack(screen, mouseX, mouseY);
            if (bomScreenStack != null && !bomScreenStack.isEmpty()) {
                return bomScreenStack;
            }

            EmiStackInteraction interaction = EmiApi.getHoveredStack((int) mouseX, (int) mouseY, true);
            if (interaction == null || interaction.isEmpty()) return null;

            EmiIngredient ingredient = interaction.getStack();
            return extractItemStack(ingredient);
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private static ItemStack getRecipeScreenStack(Object screen) {
        try {
            if (screen == null || !"dev.emi.emi.screen.RecipeScreen".equals(screen.getClass().getName())) {
                return null;
            }

            Object ingredient = screen.getClass().getMethod("getHoveredStack").invoke(screen);
            if (ingredient instanceof EmiIngredient emiIngredient) {
                return extractItemStack(emiIngredient);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private static ItemStack getBomScreenStack(Object screen, double mouseX, double mouseY) {
        try {
            if (screen == null || !"dev.emi.emi.screen.BoMScreen".equals(screen.getClass().getName())) {
                return null;
            }

            Object hover = screen.getClass()
                    .getMethod("getHoveredStack", int.class, int.class)
                    .invoke(screen, (int) mouseX, (int) mouseY);
            if (hover == null) {
                return null;
            }

            java.lang.reflect.Field stackField = hover.getClass().getDeclaredField("stack");
            stackField.setAccessible(true);
            Object ingredient = stackField.get(hover);
            if (ingredient instanceof EmiIngredient emiIngredient) {
                return extractItemStack(emiIngredient);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isSupportedScreen(@Nullable Object screen) {
        if (screen instanceof AbstractContainerScreen<?>) {
            return true;
        }
        if (screen == null) {
            return false;
        }

        String screenClassName = screen.getClass().getName();
        return "dev.emi.emi.screen.RecipeScreen".equals(screenClassName)
                || "dev.emi.emi.screen.BoMScreen".equals(screenClassName);
    }

    @Nullable
    private static ItemStack extractItemStack(@Nullable EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return null;

        for (EmiStack emiStack : ingredient.getEmiStacks()) {
            if (emiStack == null || emiStack.isEmpty()) continue;

            ItemStack stack = emiStack.getItemStack();
            if (stack != null && !stack.isEmpty()) {
                return stack.copy();
            }
        }
        return null;
    }

    @Nullable
    public static AbstractContainerScreen<?> getBackingScreen() {
        try {
            if (!isSupportedScreen(Minecraft.getInstance().screen)) {
                return null;
            }
            return EmiApi.getHandledScreen();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
