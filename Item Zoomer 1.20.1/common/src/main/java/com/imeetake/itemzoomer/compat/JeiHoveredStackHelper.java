package com.imeetake.itemzoomer.compat;

import mezz.jei.api.JeiPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@JeiPlugin
public class JeiHoveredStackHelper implements IModPlugin {

    private static IJeiRuntime jeiRuntime = null;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("itemzoomer", "jei_plugin");
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
}