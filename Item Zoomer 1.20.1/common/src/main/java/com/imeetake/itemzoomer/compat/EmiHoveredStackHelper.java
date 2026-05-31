package com.imeetake.itemzoomer.compat;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EmiHoveredStackHelper {

    @Nullable
    public static ItemStack getStack(double mouseX, double mouseY) {
        try {
            ItemStack recipeScreenStack = getRecipeScreenStack();
            if (recipeScreenStack != null && !recipeScreenStack.isEmpty()) {
                return recipeScreenStack;
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
    private static ItemStack getRecipeScreenStack() {
        try {
            Object screen = Minecraft.getInstance().screen;
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
            return EmiApi.getHandledScreen();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
