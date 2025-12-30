package com.imeetake.itemzoomer.fabric.client;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

public class ItemZoomerFabricClient implements ClientModInitializer {

    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.itemzoomer.toggle_zoom",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "key.itemzoomer.category"
        ));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ZoomedItemRenderer.cleanup();
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                ScreenKeyboardEvents.afterKeyPress(screen).register((screen1, key, scancode, modifiers) -> {
                    if (toggleKey.matches(key, scancode)) {
                        ItemZoomer.toggle();
                    }
                });

                ScreenEvents.beforeRender(screen).register((screen1, graphics, mouseX, mouseY, delta) -> {
                    ZoomedItemRenderer.beginFrame();
                });

                ScreenEvents.afterRender(screen).register((screen1, graphics, mouseX, mouseY, delta) -> {
                    ZoomedItemRenderer.render(graphics, containerScreen, mouseX, mouseY);
                });

                ScreenEvents.remove(screen).register(screen1 -> {
                    ZoomedItemRenderer.onScreenClose();
                });
            }
        });
    }
}