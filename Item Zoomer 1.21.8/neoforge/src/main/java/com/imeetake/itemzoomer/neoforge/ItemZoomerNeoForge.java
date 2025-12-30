package com.imeetake.itemzoomer.neoforge;

import com.imeetake.itemzoomer.ItemZoomer;
import com.imeetake.itemzoomer.config.ItemZoomerConfig;
import com.imeetake.itemzoomer.render.ZoomedItemPIPRenderer;
import com.imeetake.itemzoomer.render.ZoomedItemRenderState;
import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(ItemZoomer.MOD_ID)
public final class ItemZoomerNeoForge {
    public ItemZoomerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ItemZoomer.init();

        modEventBus.addListener(this::onRegisterPIPRenderers);

        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> AutoConfig.getConfigScreen(ItemZoomerConfig.class, parent).get()
        );
    }

    private void onRegisterPIPRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(ZoomedItemRenderState.class, ZoomedItemPIPRenderer::new);
    }
}