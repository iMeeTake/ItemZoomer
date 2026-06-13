package com.imeetake.itemzoomer.compat;

import mezz.jei.api.JeiPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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
            if (!isSupportedScreen(Minecraft.getInstance().screen)) {
                return null;
            }

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

    public static List<Rect2i> getExclusionBounds() {
        List<Rect2i> result = new ArrayList<>();
        if (jeiRuntime == null) return result;
        try {
            if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) {
                return result;
            }
            addOverlayArea(result, jeiRuntime.getIngredientListOverlay());
            addOverlayArea(result, jeiRuntime.getBookmarkOverlay());
        } catch (Throwable ignored) {
        }
        return result;
    }

    public static boolean shouldDeferAbove() {
        if (jeiRuntime == null) return false;
        try {
            if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) {
                return false;
            }
            Object bookmark = jeiRuntime.getBookmarkOverlay();
            if (bookmark == null) return false;
            Object displayed = bookmark.getClass().getMethod("isListDisplayed").invoke(bookmark);
            return Boolean.TRUE.equals(displayed);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void addOverlayArea(List<Rect2i> out, Object overlay) {
        try {
            if (overlay == null) return;
            Object contents = field(overlay, "contents");
            if (contents == null) return;

            Rect2i occupied = occupiedSlotArea(contents);
            if (occupied != null) {
                out.add(occupied);
            }
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    private static Rect2i occupiedSlotArea(Object contents) {
        try {
            Object grid = field(contents, "ingredientGrid");
            if (grid == null) return null;
            Object renderer = field(grid, "ingredientListRenderer");
            if (renderer == null) return null;
            Object slotsObj = field(renderer, "slots");
            if (!(slotsObj instanceof List<?> slots)) return null;

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            boolean any = false;

            for (Object slot : slots) {
                if (slot == null) continue;
                if (Boolean.TRUE.equals(slot.getClass().getMethod("isBlocked").invoke(slot))) continue;
                Object element = slot.getClass().getMethod("getOptionalElement").invoke(slot);
                if (!(element instanceof Optional<?> opt) || opt.isEmpty()) continue;

                Object area = slot.getClass().getMethod("getArea").invoke(slot);
                int x = getInt(area, "getX");
                int y = getInt(area, "getY");
                int w = getInt(area, "getWidth");
                int h = getInt(area, "getHeight");
                any = true;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x + w);
                maxY = Math.max(maxY, y + h);
            }

            if (!any) return null;
            return new Rect2i(minX, minY, maxX - minX, maxY - minY);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    @Nullable
    public static Rect2i getWindowBounds() {
        try {
            Screen screen = Minecraft.getInstance().screen;
            if (!isJeiScreen(screen)) {
                return null;
            }

            Rect2i bounds = getBoundsFromProperties(screen);
            if (bounds != null) {
                return bounds;
            }

            return getBoundsFromArea(screen);
        } catch (Throwable ignored) {
        }

        return null;
    }

    @Nullable
    public static AbstractContainerScreen<?> getBackingScreen() {
        if (jeiRuntime == null) return null;

        try {
            if (!isSupportedScreen(Minecraft.getInstance().screen)) {
                return null;
            }

            Screen parent = jeiRuntime.getRecipesGui().getParentScreen().orElse(null);
            if (parent instanceof AbstractContainerScreen<?> containerScreen) {
                return containerScreen;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static boolean isSupportedScreen(@Nullable Screen screen) {
        if (screen instanceof AbstractContainerScreen<?>) {
            return true;
        }
        return isJeiScreen(screen);
    }

    private static boolean isJeiScreen(@Nullable Screen screen) {
        if (screen == null) {
            return false;
        }
        return screen.getClass().getName().startsWith("mezz.jei.");
    }

    @Nullable
    private static Rect2i getBoundsFromProperties(Screen screen) {
        try {
            Object properties = screen.getClass().getMethod("getProperties").invoke(screen);
            if (properties == null) {
                return null;
            }

            int x = getInt(properties, "getGuiLeft", "guiLeft");
            int y = getInt(properties, "getGuiTop", "guiTop");
            int width = getInt(properties, "getGuiXSize", "guiXSize");
            int height = getInt(properties, "getGuiYSize", "guiYSize");
            return createBounds(x, y, width, height);
        } catch (Throwable ignored) {
        }

        return null;
    }

    @Nullable
    private static Rect2i getBoundsFromArea(Screen screen) {
        try {
            Object area = screen.getClass().getMethod("getArea").invoke(screen);
            if (area == null) {
                return null;
            }

            int x = getInt(area, "getX");
            int y = getInt(area, "getY");
            int width = getInt(area, "getWidth");
            int height = getInt(area, "getHeight");
            return createBounds(x, y, width, height);
        } catch (Throwable ignored) {
        }

        return null;
    }

    @Nullable
    private static Rect2i createBounds(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new Rect2i(x, y, width, height);
    }

    private static int getInt(Object target, String... methodNames) throws ReflectiveOperationException {
        for (String methodName : methodNames) {
            try {
                Object value = target.getClass().getMethod(methodName).invoke(target);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(String.join("/", methodNames));
    }
}
