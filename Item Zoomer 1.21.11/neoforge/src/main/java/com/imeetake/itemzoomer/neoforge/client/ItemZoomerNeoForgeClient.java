package com.imeetake.itemzoomer.neoforge.client;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.neoforge.ItemZoomerNeoForge;
import com.imeetake.itemzoomer.render.ZoomedItemRenderer;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

@EventBusSubscriber(modid = ItemZoomer.MOD_ID, value = Dist.CLIENT)
public class ItemZoomerNeoForgeClient {

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
        KeyEvent keyEvent = event.getKeyEvent();
        if (ItemZoomerNeoForge.ClientInit.matchesToggle(keyEvent)) {
            if (!toggleKeyDown && event.getScreen() instanceof AbstractContainerScreen<?> containerScreen && shouldHandleToggle(containerScreen)) {
                toggleKeyDown = true;
                ItemZoomer.toggle();
            }
        }
    }

    @SubscribeEvent
    public static void onKeyRelease(ScreenEvent.KeyReleased.Post event) {
        KeyEvent keyEvent = event.getKeyEvent();
        if (ItemZoomerNeoForge.ClientInit.matchesToggle(keyEvent)) {
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
}
