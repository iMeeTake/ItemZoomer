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
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ReiHoveredStackHelper {

    @Nullable
    public static ItemStack getStack(double mouseX, double mouseY) {
        try {
            ItemStack focusedScreenStack = getFocusedScreenStack(mouseX, mouseY);
            if (focusedScreenStack != null) {
                return focusedScreenStack;
            }

            ItemStack displayScreenStack = getDisplayScreenStack(mouseX, mouseY);
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
    private static ItemStack getFocusedScreenStack(double mouseX, double mouseY) {
        try {
            Screen screen = Minecraft.getInstance().screen;
            if (screen == null) return null;

            EntryStack<?> focused = ScreenRegistry.getInstance().getFocusedStack(screen, new Point(mouseX, mouseY));
            if (focused != null && !focused.isEmpty()) {
                return extractItemStack(focused);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    private static ItemStack getDisplayScreenStack(double mouseX, double mouseY) {
        try {
            Screen screen = Minecraft.getInstance().screen;
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

    @Nullable
    public static Rect2i getWindowBounds() {
        try {
            Screen screen = Minecraft.getInstance().screen;
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
