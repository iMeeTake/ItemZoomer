package com.imeetake.itemzoomer.fabric.client;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.config.EnumCycleProvider;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

public class ItemZoomerFabricClient implements ClientModInitializer {

    private static KeyMapping toggleKey;
    private static boolean toggleKeyDown;

    @Override
    public void onInitializeClient() {
        registerConfigGui();

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
                        if (!toggleKeyDown && shouldHandleToggle(containerScreen)) {
                            toggleKeyDown = true;
                            ItemZoomer.toggle();
                        }
                    }
                });

                ScreenKeyboardEvents.afterKeyRelease(screen).register((screen1, key, scancode, modifiers) -> {
                    if (toggleKey.matches(key, scancode)) {
                        toggleKeyDown = false;
                    }
                });
            }

            ScreenEvents.beforeRender(screen).register((screen1, graphics, mouseX, mouseY, delta) -> {
                ZoomedItemRenderer.beginFrame();
            });

            ScreenEvents.afterRender(screen).register((screen1, graphics, mouseX, mouseY, delta) -> {
                ZoomedItemRenderer.render(graphics, screen, mouseX, mouseY);
            });

            ScreenEvents.remove(screen).register(screen1 -> {
                toggleKeyDown = false;
                ZoomedItemRenderer.onScreenClose();
            });
        });
    }

    private static boolean shouldHandleToggle(AbstractContainerScreen<?> screen) {
        return !(screen.getFocused() instanceof EditBox);
    }

    private static void registerConfigGui() {
        GuiRegistry registry = AutoConfig.getGuiRegistry(ItemZoomerConfig.class);
        registry.registerPredicateProvider(new EnumCycleProvider(), field -> field.getType().isEnum());
    }
}
