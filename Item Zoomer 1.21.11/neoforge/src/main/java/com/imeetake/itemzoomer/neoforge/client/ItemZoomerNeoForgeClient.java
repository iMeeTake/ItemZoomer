package com.imeetake.itemzoomer.neoforge.client;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.neoforge.ItemZoomerNeoForge;
import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

@EventBusSubscriber(modid = ItemZoomer.MOD_ID, value = Dist.CLIENT)
public class ItemZoomerNeoForgeClient {

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?>) {
            ZoomedItemRenderer.beginFrame();
        }
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            ZoomedItemRenderer.render(
                    event.getGuiGraphics(),
                    containerScreen,
                    event.getMouseX(),
                    event.getMouseY()
            );
        }
    }

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Post event) {
        if (ItemZoomerNeoForge.toggleKey != null) {
            KeyEvent keyEvent = new KeyEvent(event.getKeyCode(), event.getScanCode(), event.getModifiers());
            if (ItemZoomerNeoForge.toggleKey.matches(keyEvent)) {
                ItemZoomer.toggle();
            }
        }
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?>) {
            ZoomedItemRenderer.onScreenClose();
        }
    }

    @SubscribeEvent
    public static void onGameShutdown(GameShuttingDownEvent event) {
        ZoomedItemRenderer.cleanup();
    }
}