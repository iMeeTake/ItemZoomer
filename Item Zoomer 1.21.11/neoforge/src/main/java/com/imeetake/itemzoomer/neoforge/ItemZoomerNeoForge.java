package com.imeetake.itemzoomer.neoforge;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.imeetake.itemzoomer.render.ZoomedItemPIPRenderer;
import com.imeetake.itemzoomer.render.ZoomedItemRenderState;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.lwjgl.glfw.GLFW;

@Mod(ItemZoomer.MOD_ID)
public final class ItemZoomerNeoForge {

    public static KeyMapping toggleKey;

    public ItemZoomerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ItemZoomer.init();

        modEventBus.addListener(this::onRegisterPIPRenderers);
        modEventBus.addListener(this::onRegisterKeyMappings);

        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> AutoConfigClient.getConfigScreen(ItemZoomerConfig.class, parent).get()
        );
    }

    private void onRegisterPIPRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(ZoomedItemRenderState.class, ZoomedItemPIPRenderer::new);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        toggleKey = new KeyMapping(
                "key.itemzoomer.toggle_zoom",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("itemzoomer", "category"))
        );

        event.register(toggleKey);
    }
}