package com.imeetake.itemzoomer.compat;

import mezz.jei.api.JeiPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@JeiPlugin
public class JeiHoveredStackHelper implements IModPlugin {

    private static IJeiRuntime jeiRuntime = null;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("itemzoomer", "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    @Override
    public void onRuntimeUnavailable() {
        jeiRuntime = null;
    }

    @Nullable
    public static ItemStack getStack(double mouseX, double mouseY) {
        if (jeiRuntime == null) return null;

        try {
            ItemStack result = jeiRuntime.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
            if (result != null) return result;

            result = jeiRuntime.getBookmarkOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
            if (result != null) return result;

            Optional<ItemStack> recipeResult = jeiRuntime.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
            if (recipeResult.isPresent()) return recipeResult.get();
        } catch (Exception ignored) {
        }

        return null;
    }

    @Nullable
    public static AbstractContainerScreen<?> getBackingScreen() {
        if (jeiRuntime == null) return null;

        try {
            Screen parent = jeiRuntime.getRecipesGui().getParentScreen().orElse(null);
            if (parent instanceof AbstractContainerScreen<?> containerScreen) {
                return containerScreen;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}
