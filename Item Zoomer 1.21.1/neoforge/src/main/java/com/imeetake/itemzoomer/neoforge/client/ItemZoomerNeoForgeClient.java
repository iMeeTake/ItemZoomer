package com.imeetake.itemzoomer.neoforge.client;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ItemZoomer.MOD_ID, value = Dist.CLIENT)
public class ItemZoomerNeoForgeClient {

    private static KeyMapping toggleKey;
    private static boolean toggleKeyDown;

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        ZoomedItemRenderer.beginFrame();
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        ZoomedItemRenderer.render(
                event.getGuiGraphics(),
                event.getScreen(),
                event.getMouseX(),
                event.getMouseY()
        );
    }

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Post event) {
        if (toggleKey != null && toggleKey.matches(event.getKeyCode(), event.getScanCode())) {
            if (!toggleKeyDown && event.getScreen() instanceof AbstractContainerScreen<?> containerScreen && shouldHandleToggle(containerScreen)) {
                toggleKeyDown = true;
                ItemZoomer.toggle();
            }
        }
    }

    @SubscribeEvent
    public static void onKeyRelease(ScreenEvent.KeyReleased.Post event) {
        if (toggleKey != null && toggleKey.matches(event.getKeyCode(), event.getScanCode())) {
            toggleKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        toggleKeyDown = false;
        ZoomedItemRenderer.onScreenClose();
    }

    @SubscribeEvent
    public static void onGameShutdown(GameShuttingDownEvent event) {
        toggleKeyDown = false;
        ZoomedItemRenderer.cleanup();
    }

    private static boolean shouldHandleToggle(AbstractContainerScreen<?> screen) {
        return !(screen.getFocused() instanceof EditBox);
    }

    @EventBusSubscriber(modid = ItemZoomer.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            toggleKey = new KeyMapping(
                    "key.itemzoomer.toggle_zoom",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_Z,
                    "key.itemzoomer.category"
            );
            event.register(toggleKey);
        }
    }
}
