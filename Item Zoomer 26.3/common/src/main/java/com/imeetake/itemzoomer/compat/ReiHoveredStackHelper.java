package com.imeetake.itemzoomer.compat;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.screen.DisplayScreen;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.overlay.OverlayListWidget;
import me.shedaniel.rei.api.client.overlay.ScreenOverlay;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReiHoveredStackHelper {

    public static List<Rect2i> getExclusionBounds() {
        List<Rect2i> result = new ArrayList<>();
        try {
            if (!(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?>)) {
                return result;
            }
            Optional<ScreenOverlay> overlayOpt = REIRuntime.getInstance().getOverlay();
            if (overlayOpt.isEmpty()) {
                return result;
            }
            overlayOpt.get().getFavoritesList().ifPresent(favorites -> {
                addRegionOccupied(result, invoke(favorites, "getRegion"));
                addRegionOccupied(result, invoke(favorites, "getSystemRegion"));
            });
        } catch (Throwable ignored) {
        }
        return result;
    }

    @Nullable
    private static Object invoke(Object target, String method) {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void addRegionOccupied(List<Rect2i> out, Object region) {
        if (region == null) return;
        try {
            Object regionBounds = region.getClass().getMethod("getBounds").invoke(region);
            int rx = rectInt(regionBounds, "getX");
            int ry = rectInt(regionBounds, "getY");
            int rMaxX = rx + rectInt(regionBounds, "getWidth");
            int rMaxY = ry + rectInt(regionBounds, "getHeight");
            if (rMaxX <= rx || rMaxY <= ry) return;

            Field entriesField = region.getClass().getDeclaredField("entriesList");
            entriesField.setAccessible(true);
            Object entriesObj = entriesField.get(region);
            if (!(entriesObj instanceof List<?> entries) || entries.isEmpty()) return;

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            boolean any = false;

            for (Object entry : entries) {
                if (entry == null) continue;
                Object eb = entry.getClass().getMethod("getBounds").invoke(entry);
                int x0 = Math.max(rectInt(eb, "getX"), rx);
                int y0 = Math.max(rectInt(eb, "getY"), ry);
                int x1 = Math.min(rectInt(eb, "getX") + rectInt(eb, "getWidth"), rMaxX);
                int y1 = Math.min(rectInt(eb, "getY") + rectInt(eb, "getHeight"), rMaxY);
                if (x1 <= x0 || y1 <= y0) continue;
                any = true;
                minX = Math.min(minX, x0);
                minY = Math.min(minY, y0);
                maxX = Math.max(maxX, x1);
                maxY = Math.max(maxY, y1);
            }

            if (any) {
                out.add(new Rect2i(minX, minY, maxX - minX, maxY - minY));
            }
        } catch (Throwable ignored) {
        }
    }

    private static int rectInt(Object rect, String method) throws ReflectiveOperationException {
        return ((Number) rect.getClass().getMethod(method).invoke(rect)).intValue();
    }

    @Nullable
    public static ItemStack getStack(double mouseX, double mouseY) {
        try {
            Screen screen = Minecraft.getInstance().gui.screen();
            if (!isSupportedScreen(screen)) {
                return null;
            }

            ItemStack focusedScreenStack = getFocusedScreenStack(screen, mouseX, mouseY);
            if (focusedScreenStack != null) {
                return focusedScreenStack;
            }

            ItemStack displayScreenStack = getDisplayScreenStack(screen, mouseX, mouseY);
            if (displayScreenStack != null) {
                return displayScreenStack;
            }

            Optional<ScreenOverlay> overlayOpt = REIRuntime.getInstance().getOverlay();
            if (overlayOpt.isEmpty()) return null;

            ScreenOverlay overlay = overlayOpt.get();

            OverlayListWidget entryList = overlay.getEntryList();
            EntryStack<?> focused = entryList.getFocusedStack();
            if (focused != null && !focused.isEmpty()) {
                return extractItemStack(focused);
            }

            Optional<OverlayListWidget> favoritesOpt = overlay.getFavoritesList();
            if (favoritesOpt.isPresent()) {
                EntryStack<?> favFocused = favoritesOpt.get().getFocusedStack();
                if (favFocused != null && !favFocused.isEmpty()) {
                    return extractItemStack(favFocused);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private static ItemStack getFocusedScreenStack(Screen screen, double mouseX, double mouseY) {
        try {
            EntryStack<?> focused = ScreenRegistry.getInstance().getFocusedStack(screen, new Point(mouseX, mouseY));
            if (focused != null && !focused.isEmpty()) {
                return extractItemStack(focused);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private static ItemStack getDisplayScreenStack(Screen screen, double mouseX, double mouseY) {
        try {
            if (!(screen instanceof DisplayScreen)) return null;

            Slot slot = findHoveredSlot(screen, mouseX, mouseY, 0);
            if (slot != null) {
                EntryStack<?> entry = slot.getCurrentEntry();
                if (entry != null && !entry.isEmpty()) {
                    return extractItemStack(entry);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isSupportedScreen(@Nullable Screen screen) {
        return screen instanceof AbstractContainerScreen<?> || screen instanceof DisplayScreen;
    }

    @Nullable
    public static Rect2i getWindowBounds() {
        try {
            Screen screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof DisplayScreen displayScreen) {
                Rectangle bounds = displayScreen.getBounds();
                if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
                    return new Rect2i(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private static Slot findHoveredSlot(GuiEventListener node, double mouseX, double mouseY, int depth) {
        if (depth > 16) return null;

        if (node instanceof Slot slot && slot.containsMouse(mouseX, mouseY)) {
            EntryStack<?> entry = slot.getCurrentEntry();
            if (entry != null && !entry.isEmpty()) {
                return slot;
            }
        }

        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                Slot found = findHoveredSlot(child, mouseX, mouseY, depth + 1);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Nullable
    private static ItemStack extractItemStack(EntryStack<?> entryStack) {
        if (entryStack.getType() == VanillaEntryTypes.ITEM) {
            Object value = entryStack.getValue();
            if (value instanceof ItemStack stack) {
                return stack.copy();
            }
        }
        return null;
    }
}
