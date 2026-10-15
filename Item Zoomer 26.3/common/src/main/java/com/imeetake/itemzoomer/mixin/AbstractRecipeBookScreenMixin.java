package com.imeetake.itemzoomer.mixin;

import com.imeetake.itemzoomer.accessor.RecipeBookScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin implements RecipeBookScreenAccessor {

    @Shadow
    @Final
    private RecipeBookComponent<?> recipeBookComponent;

    @Override
    public boolean itemzoomer$isRecipeBookVisible() {
        return this.recipeBookComponent != null && this.recipeBookComponent.isVisible();
    }
}